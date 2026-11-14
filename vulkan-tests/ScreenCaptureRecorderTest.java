import dev.reflash.core.DirectShowAudioDevices;
import dev.reflash.core.ScreenCaptureRecorder;
import dev.reflash.core.Preferences;
import java.nio.file.Path;
import java.util.List;

/** Pure command/option tests. No webcam, microphone, display, encoder or network access. */
public final class ScreenCaptureRecorderTest {
    public static void main(String[] args) {
        String listing = """
            [dshow @ 123] DirectShow video devices (some may be both video and audio devices)
            [dshow @ 123]   "Game Capture" (video)
            [dshow @ 123] DirectShow audio devices
            [dshow @ 123]   "Microphone (USB Headset)" (audio)
            [dshow @ 123]      Alternative name "@device_cm_..."
            [dshow @ 123]   "Stereo Mix (Realtek)" (audio)
            [dshow @ 123]   "Microphone (USB Headset)" (audio)
            """;
        List<String> parsed = DirectShowAudioDevices.parseAudioDevices(listing);
        check(parsed.equals(List.of("Microphone (USB Headset)", "Stereo Mix (Realtek)")), "audio device parsing");
        check(DirectShowAudioDevices.looksLikeLoopback(parsed.get(1)), "loopback classification");
        check(!DirectShowAudioDevices.looksLikeLoopback(parsed.getFirst()), "mic not treated as loopback");
        check(DirectShowAudioDevices.cycle(parsed, parsed.getFirst()).equals(parsed.get(1)), "selection cycle");
        check(DirectShowAudioDevices.cycle(List.of(), "anything").isBlank(), "no fabricated devices");

        Preferences p = new Preferences(Path.of("unused.properties"));
        List<String> silent = ScreenCaptureRecorder.command("ffmpeg", Path.of("capture.mkv"), p);
        check(silent.contains("desktop") && silent.contains("-an"), "silent capture only when both audio toggles OFF");
        check(!silent.contains("dshow"), "no microphone access without opt-in");
        p.setCaptureMicrophone(true);
        p.setScreenMicDevice("Microphone (USB Headset)");
        List<String> micOnly = ScreenCaptureRecorder.command("ffmpeg", Path.of("capture.mkv"), p);
        check(micOnly.contains("audio=Microphone (USB Headset)"), "selected microphone");
        check(micOnly.contains("1:a:0") && !micOnly.contains("-an"), "single audio map");
        p.setCaptureDesktopAudio(true);
        p.setScreenDesktopDevice("Stereo Mix (Realtek)");
        List<String> mixed = ScreenCaptureRecorder.command("ffmpeg", Path.of("capture.mkv"), p);
        check(mixed.contains("audio=Stereo Mix (Realtek)"), "selected desktop source");
        check(mixed.contains("[1:a:0][2:a:0]amix=inputs=2:duration=longest:dropout_transition=0[a]"),"audio mix mapping");
        check(mixed.getLast().endsWith("capture.mkv"), "video output filename");
        check(mixed.contains("ultrafast"), "low overhead CPU preset selected");
        System.out.println("ScreenCaptureRecorderTest PASS");
    }
    private static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
}
