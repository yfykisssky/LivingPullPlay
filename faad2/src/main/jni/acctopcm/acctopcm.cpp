#include "acctopcm.h"
#include <stdio.h>
#include <memory.h>
#include <android/log.h>
#include "tools.h"
#include "tools.c"

NeAACDecHandle decoder;
NeAACDecFrameInfo frame_info;
bool decoderIsInit = false;
unsigned long sampleRate;
unsigned char channels;

int initDecoder(unsigned char defObjectType,
                unsigned char fmtBitType, unsigned long defSampleRate,
                unsigned char defChannels) {

    decoder = 0;
    decoder = NeAACDecOpen();

    NeAACDecConfigurationPtr conf = NeAACDecGetCurrentConfiguration(decoder);
    conf->defObjectType = defObjectType;
    conf->defSampleRate = defSampleRate;
    conf->outputFormat = fmtBitType;
    conf->dontUpSampleImplicitSBR = 1;

    NeAACDecSetConfiguration(decoder, conf);

    sampleRate = defSampleRate;
    channels = defChannels;

    decoderIsInit = false;

    return 0;
}

int unInitDecoder() {
    NeAACDecClose(decoder);
    decoderIsInit = false;
    return 0;
}

//转换
int convertToPcm(unsigned char *bufferAAC,
                 size_t buf_sizeAAC,
                 unsigned char *bufferOutPcm) {
    unsigned char *pcm_data = NULL;

    if (!decoderIsInit) {
        NeAACDecInit(decoder, bufferAAC, buf_sizeAAC, &sampleRate, &channels);
        decoderIsInit = true;
    }

    pcm_data = (unsigned char *) NeAACDecDecode(decoder, &frame_info, bufferAAC, buf_sizeAAC);

    if (frame_info.error > 0) {
        LOGE("ERROR:%d", frame_info.error);
        return 0;
    } else if (pcm_data && frame_info.samples > 0) {
        //PCMBufferSize = 采样率*采样时间*采样位深/8*通道数
        int buf_sizePCM = frame_info.samples * frame_info.channels;
        memcpy(bufferOutPcm, pcm_data, buf_sizePCM);
        return buf_sizePCM;
    }
    return 0;

}

