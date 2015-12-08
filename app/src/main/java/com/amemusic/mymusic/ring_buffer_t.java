package com.amemusic.mymusic;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by klentz on 11/17/15.
 */

public class ring_buffer_t {

    public interface writer_i {
       int write(byte [] buffer, int offset, int frames_per_period) throws IOException;
    }

    public interface reader_i{
        int read(byte [] buffer, int offset, int frames_per_period) throws IOException;
    }

    private final Lock lock_ = new ReentrantLock();
    private Condition full_  = lock_.newCondition();
    private Condition empty_ = lock_.newCondition();

    private int size_;
    private int read_ptr_ = 0;
    private int write_ptr_ = 0;
    boolean empty_p_ = true;
    private int min_threshold_ = 0;
    private int max_threshold_ = size_;

    byte[] buffer_;
    int frames_per_period_;

    ring_buffer_t(int periods, int frames_per_period) {
        buffer_ = new byte [periods * frames_per_period];
        frames_per_period_ = frames_per_period;
        size_ = buffer_.length;
    }

    private int get_samples_available(){
        if(empty_p_){
            return 0;
        }
        else {
            return write_ptr_ > read_ptr_ ? write_ptr_ - read_ptr_ : size_ - read_ptr_ + write_ptr_;
        }
    }

    int read(reader_i reader) throws InterruptedException, IOException{

        lock_.lock();
        try {
            while(get_samples_available() <= min_threshold_){
                empty_.await();
            }
        }
        finally {
            lock_.unlock();
        }

        int ret;
        if(read_ptr_ + frames_per_period_ <= size_) {
            ret = reader.read(buffer_, read_ptr_, frames_per_period_);
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

    int write(writer_i writer) throws InterruptedException, IOException{

        lock_.lock();
        try {
            while(get_samples_available() >= max_threshold_){
                full_.await();
            }
        }
        finally {
            lock_.unlock();
        }

        int ret;
        if(write_ptr_ + frames_per_period_ <= size_) {
            ret = writer.write(buffer_, write_ptr_, frames_per_period_);
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