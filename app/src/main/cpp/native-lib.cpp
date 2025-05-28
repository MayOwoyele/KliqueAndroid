#include <jni.h>
#include <android/log.h>
#include <string>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
#include <libavutil/channel_layout.h>
}

#define LOG_TAG "NativeFFmpeg"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO , LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_justself_klique_FFmpegNative_convertPcmToM4a(
        JNIEnv*  env,
        jobject  /* this */,
        jstring  jInputPath,
        jstring  jOutputPath)
{
    /* ---------- ALL variables first ---------- */
    const char*       inPath  = env->GetStringUTFChars(jInputPath , nullptr);
    const char*       outPath = env->GetStringUTFChars(jOutputPath, nullptr);

    AVFormatContext*  fmtCtx  = nullptr;
    const AVCodec*          codec   = nullptr;
    AVStream*         stream  = nullptr;
    AVCodecContext*   cCtx    = nullptr;
    SwrContext*       swr     = nullptr;
    FILE*             inFile  = nullptr;
    int64_t           pts     = 0;
    const int frameSamp = 1024;
    AVChannelLayout in_layout, out_layout;
    av_channel_layout_from_mask(&in_layout,  AV_CH_LAYOUT_MONO);
    av_channel_layout_from_mask(&out_layout, AV_CH_LAYOUT_MONO);
    int swr_err = swr_alloc_set_opts2(
            /* ctx */           &swr,
            /* out_ch_layout */ &out_layout,
            /* out_fmt */       cCtx->sample_fmt,
            /* out_rate */      cCtx->sample_rate,
            /* in_ch_layout */  &in_layout,
            /* in_fmt */        AV_SAMPLE_FMT_S16,
            /* in_rate */       cCtx->sample_rate,
            /* log_offset */    0,
            /* log_ctx */       nullptr
    );

    /* ---------- 1. allocate fmtCtx ---------- */
    if (avformat_alloc_output_context2(&fmtCtx, nullptr, nullptr, outPath) < 0) {
        ALOGE("alloc output ctx failed");
        goto fail;
    }

    /* ---------- 2. find encoder ---------- */
    codec = avcodec_find_encoder(AV_CODEC_ID_AAC);
    if (!codec) { ALOGE("AAC encoder not found"); goto fail; }

    /* ---------- 3. new stream ---------- */
    stream = avformat_new_stream(fmtCtx, codec);
    if (!stream) { ALOGE("new_stream failed"); goto fail; }
    cCtx = avcodec_alloc_context3(codec);
    cCtx->codec_type     = AVMEDIA_TYPE_AUDIO;
    cCtx->sample_fmt     = AV_SAMPLE_FMT_FLTP;
    cCtx->sample_rate    = 44100;
    cCtx->bit_rate       = 128000;
    if (fmtCtx->oformat->flags & AVFMT_GLOBALHEADER)
        cCtx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

    if (avcodec_open2(cCtx, codec, nullptr) < 0) { ALOGE("open2 fail"); goto fail; }

    avcodec_parameters_from_context(stream->codecpar, cCtx);

    /* ---------- 5. open file ---------- */
    if (!(fmtCtx->oformat->flags & AVFMT_NOFILE))
        if (avio_open(&fmtCtx->pb, outPath, AVIO_FLAG_WRITE) < 0) { ALOGE("avio open fail"); goto fail; }

    /* ---------- 6. header ---------- */
    if (avformat_write_header(fmtCtx, nullptr) < 0) { ALOGE("write header fail"); goto fail; }
    /* ---------- 7. resampler ---------- */
    if (swr_err < 0 || !swr || swr_init(swr) < 0) {
        ALOGE("Could not initialize resampler");
        goto fail;
    }

    /* ---------- 8. open pcm ---------- */
    inFile = fopen(inPath, "rb");
    if (!inFile) { ALOGE("open pcm fail"); goto fail; }

    /* ---------- 9. loop ---------- */

    while (!feof(inFile)) {
        int16_t buf[frameSamp];
        int read = fread(buf, sizeof(int16_t), frameSamp, inFile);
        if (read <= 0) break;

        AVFrame* f = av_frame_alloc();
        f->nb_samples     = read;
        f->format         = cCtx->sample_fmt;
        av_channel_layout_copy(&f->ch_layout, &in_layout);
        f->sample_rate    = cCtx->sample_rate;
        av_frame_get_buffer(f, 0);

        const uint8_t* inData[1] = { reinterpret_cast<uint8_t*>(buf) };
        swr_convert(swr, f->data, read, inData, read);

        f->pts = pts; pts += read;

        if (avcodec_send_frame(cCtx, f) == 0) {
            AVPacket pkt; av_init_packet(&pkt);
            while (avcodec_receive_packet(cCtx, &pkt) == 0) {
                pkt.stream_index = stream->index;
                av_interleaved_write_frame(fmtCtx, &pkt);
                av_packet_unref(&pkt);
            }
        }
        av_frame_free(&f);
    }

    /* ---------- 10. trailer ---------- */
    av_write_trailer(fmtCtx);

    /* ---------- success cleanup ---------- */
    if (inFile) fclose(inFile);
    if (swr) swr_free(&swr);
    if (cCtx) avcodec_free_context(&cCtx);
    if (!(fmtCtx->oformat->flags & AVFMT_NOFILE)) avio_closep(&fmtCtx->pb);
    if (fmtCtx) avformat_free_context(fmtCtx);
    env->ReleaseStringUTFChars(jInputPath , inPath);
    env->ReleaseStringUTFChars(jOutputPath, outPath);
    return JNI_TRUE;

/* ===== unified fail path ===== */
    fail:
    if (inFile) fclose(inFile);
    if (swr) swr_free(&swr);
    if (cCtx) avcodec_free_context(&cCtx);
    if (!(fmtCtx && fmtCtx->oformat && (fmtCtx->oformat->flags & AVFMT_NOFILE)))
        if (fmtCtx && fmtCtx->pb) avio_closep(&fmtCtx->pb);
    if (fmtCtx) avformat_free_context(fmtCtx);
    env->ReleaseStringUTFChars(jInputPath , inPath);
    env->ReleaseStringUTFChars(jOutputPath, outPath);
    return JNI_FALSE;
}

} // extern "C"