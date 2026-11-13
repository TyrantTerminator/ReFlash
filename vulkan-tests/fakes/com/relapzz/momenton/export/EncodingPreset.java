package com.relapzz.momenton.export;public enum EncodingPreset {MP4_DEFAULT;public String arguments(){return "-y -f rawvideo -c:v libx264 -crf 18 \"%FILENAME%\"";}}
