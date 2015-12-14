package com.amemusic.mymusic;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by klentz on 11/17/15.
 */

public class ring_buffer_t<T> {

    public interface writer_i<T>{
        int write(T [] buffer, int offset, int length) throws IOException, eof_t;
    }

    public interface reader_i<T>{
        int read(T [] buffer, int offset, int length) throws IOException, eof_t;
    }

    public interface factory_i<T>{
        T[] new_inst(int size);
    }

    private final Lock lock_ = new ReentrantLock();
    private Condition full_  = lock_.newCondition();
    private Condition empty_ = lock_.newCondition();

    private int size_ = 0;
    private int read_ptr_ = 0;
    private int write_ptr_ = 0;
    boolean empty_p_ = true;
    private int min_threshold_;
    private int max_threshold_;

    T[] buffer_;
    int frames_per_period_;

    ring_buffer_t(factory_i<T> factory, int frames_per_period, int periods) {

        size_ = periods * frames_per_period;
        frames_per_period_ = frames_per_period;

        min_threshold_ = 1;
        max_threshold_ = size_ - frames_per_period_;

        buffer_ = factory.new_inst(size_);

        for(int idx = 0; idx < size_; ++idx){
            buffer_[idx] = null;
        }
    }

    private int get_samples_available(){
        if(empty_p_){
            return 0;
        }
        else {
            return write_ptr_ > read_ptr_ ? write_ptr_ - read_ptr_ : size_ - read_ptr_ + write_ptr_;
        }
    }

    int read(reader_i<T> reader) throws InterruptedException, IOException, eof_t{

        int samples;

        lock_.lock();
        try {
            for(samples = get_samples_available(); samples < min_threshold_; samples=get_samples_available()){
                empty_.await();
            }
        }
        finally {
            lock_.unlock();
        }

        int ret;
        if(read_ptr_ + samples <= size_) {
            ret = reader.read(buffer_, read_ptr_, samples);
        }
        else {
            ret = reader.read(buffer_, read_ptr_, size_ - read_ptr_);
        }

        lock_.lock();
        try {
            read_ptr_ = (read_ptr_ + ret) % size_;
            if(read_ptr_ == write_ptr_){
                empty_p_ = true;
            }

            full_.signal();
        }
        finally {
            lock_.unlock();
        }

        return ret;
    }

    int write(writer_i<T> writer) throws InterruptedException, IOException, eof_t{

        int samples;

        lock_.lock();
        try {
            for(samples = get_samples_available(); samples > max_threshold_; samples=get_samples_available()){
                full_.await();
            }
        }
        finally {
            lock_.unlock();
        }

        samples = size_ - max_threshold_;

        int ret;
        if(write_ptr_ + samples <= size_) {
            ret = writer.write(buffer_, write_ptr_, samples);
        }
        else {
            ret = writer.write(buffer_, write_ptr_, size_ - write_ptr_);
        }

        lock_.lock();
        try {
            write_ptr_ = (write_ptr_ + ret) % size_;
            empty_p_ = false;
            empty_.signal();
        }
        finally{
            lock_.unlock();
        }

        return ret;
    }
}