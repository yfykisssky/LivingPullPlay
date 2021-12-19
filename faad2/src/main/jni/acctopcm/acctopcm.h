#ifndef LIVINGPULLPLAY_AACTOPCM_H
#define LIVINGPULLPLAY_AACTOPCM_H

#pragma once

#include <faad.h>

//初始化
int initDecoder(unsigned char defObjectType,
                unsigned char fmtBitType,
                unsigned long defSampleRate,
                unsigned char defChannels);

//反初始化
int unInitDecoder();

//转换
int convertToPcm(unsigned char *bufferAAC,
                 size_t buf_sizeAAC,
                 unsigned char *bufferPCM,
                 size_t buf_sizePCM);

#endif //LIVINGPULLPLAY_AACTOPCM_H
