package com.relapzz.momenton.export;import java.nio.file.Path;import java.util.List;import com.relapzz.momenton.edit.Clip;
public record ExportSettings(Path replay,List<Clip> clips,int width,int height,int fps,Path output,String ffmpegCommand,String ffmpegArgs,int bitrate,boolean drawCursor,float gameVolume,float micVolume){}
