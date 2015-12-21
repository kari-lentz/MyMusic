package com.amemusic.mymusic;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;

/**
 * Created by klentz on 12/17/15.
 */
public class mp3_iter_t {

    public class parse_fault_t extends Exception{
        parse_fault_t(String msg){
            super(msg);
        }
    }

    public interface reader_i{
        int read(byte dest[], int offset, int length) throws IOException;
    }

    private reader_i reader_;
    private byte [] frame_bytes_ = null;

    int extract_frame_header()
    {
        return  (((int) frame_bytes_[0] << 24) & 0xff000000) | (((int) frame_bytes_[1] << 16) & 0x00ff0000) | (((int) frame_bytes_[2] << 8) & 0x0000ff00) | (((int) frame_bytes_[3] & 0x000000ff));
    }

    private static int extract_num_channels(int frame_header){
        final int kmask_channels = 0x000000C0;
        return ((frame_header & kmask_channels) >> 6) == 0x3 ? 1 : 2;
    }

    private static int extract_sample_rate(int frame_header)
    {
        final int kmask_sampling = 0x00000C00; //the mask for the bits that contain the sampling rate
        int temp; //temporary variable
        int freq; //the pcm sampling frequecy

        temp = (frame_header & kmask_sampling ) >> 10;

        switch( temp )
        {
            case 0x00:{
                freq = 44100;
                break;
            }
            case 0x01:{
                freq = 48000;
                break;
            }
            case 0x02:{
                freq = 32000;
                break;
            }
            default:{
                freq = 0;
                break;
            }
        }

        return freq;
    }

    private static int extract_bit_rate(int frame_header)
    {
        int bit_rate; // array of bit rates
        int temp; //temporary variable
        int bit_rates [] = { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128,160, 192, 244, 256, 320, 0 };
        int mask_bit_rate = 0x0000F000; //the mask for the bits contain the bitrate

        temp = (frame_header & mask_bit_rate ) >> 12;

        if( temp >= 0 && temp < bit_rates.length ) {
            bit_rate = bit_rates[ temp ] * 1000;
        }
        else {
            bit_rate = 0;
        }

        return bit_rate;
    }

    private static int extract_frame_size(int frame_header) {
        int bit_rate; // the bit rate in the frame
        int freq; //the finally frequency track is played at
        int frame_size; //the size of the mp3
        int kpadding = 0x00000200;

        bit_rate = extract_bit_rate(frame_header);

        if (bit_rate == 0) {
            frame_size = 0;
        }
        else {
            freq = extract_sample_rate(frame_header);

            if (freq > 0) {
                frame_size = 144 * bit_rate / freq + ((frame_header & kpadding) > 0 ? 1 : 0);
            }
            else {
                frame_size = 0;
            }
        }

        return frame_size;
    }

    boolean verify_frame_header(int frame_header)
    {
        final int kframe_header = 0xffe20000;

        return (frame_header & kframe_header) == kframe_header;
    }

    void alloc(int num_bytes){
        int header = (frame_bytes_ != null) ? extract_frame_header() : 0;

        frame_bytes_ = new byte[num_bytes];

        frame_bytes_[0] = Integer.valueOf((header & 0xff000000) >> 24).byteValue();
        frame_bytes_[1] = Integer.valueOf((header & 0x00ff0000) >> 16).byteValue();
        frame_bytes_[2] = Integer.valueOf((header & 0x0000ff00) >> 8).byteValue();
        frame_bytes_[3] = Integer.valueOf((header & 0x000000ff)).byteValue();
    }

    mp3_iter_t(reader_i reader) {
        reader_ = reader;
    }

    mp3_iter_t(byte[] frame_bytes){
       frame_bytes_ = frame_bytes;
    }

    int get_num_channels(){
        return extract_num_channels(extract_frame_header());
    }

    int get_bit_rate(){
        return extract_bit_rate(extract_frame_header());
    }

    int get_sample_rate(){
        return extract_sample_rate(extract_frame_header());
    }

    int get_frame_size(){
        return extract_frame_size(extract_frame_header());
    }

    byte[] get_frame_bytes(){
        return frame_bytes_;
    }

    byte[] next() throws parse_fault_t, IOException
    {
        byte[] ret;

        if(frame_bytes_ == null){
            alloc(4);
        }

        int ret_bytes = reader_.read(frame_bytes_, 0, 4);

        if(ret_bytes <= -1) {
            ret =  null;
        }
        else if(ret_bytes < 4){
            throw new parse_fault_t("Incomplete frame header");
        }
        else {
            int header = extract_frame_header();

            if (!verify_frame_header(header)) {
                throw new parse_fault_t("invalid frame_header");
            }

            int size = extract_frame_size(header);
            if (size > 4) {

                if (frame_bytes_.length < size) {
                    alloc(size);
                }

                int remaining = size - 4;
                ret_bytes = reader_.read(frame_bytes_, 4, remaining);
                if (ret_bytes < remaining) {
                    throw new parse_fault_t(String.format("incomplete frame data:expected %d bytes:got %d bytes", remaining, ret_bytes));
                }
            } else {
                throw new parse_fault_t("frame needs to be bigger than 4 bytes");
            }

            ret = frame_bytes_;
        }

        return ret;
    }

    String dump()
    {
        StringWriter writer = new StringWriter();

        int header = extract_frame_header();
        if(verify_frame_header(header))
        {
            writer.write("MP3 dump:");
            writer.write(String.format("num-channels=%d:",extract_num_channels(header)));
            writer.write(String.format("sample-rate=%d:",extract_sample_rate(header)));
            writer.write(String.format("bit-rate=%d:", extract_bit_rate(header)));
            writer.write(String.format("frame-size=%d:", extract_frame_size(header)));
        }
        else
        {
            writer.write("dump -> invalid header");
        }

        return writer.toString();
    }

}
