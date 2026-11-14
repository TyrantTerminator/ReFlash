import dev.reflash.core.RecordingSessionDiagnostics;
import java.nio.file.Files;

public class RecordingSessionDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        var root = Files.createTempDirectory("reflash-session-diagnostics");
        try {
            var log = root.resolve("reflash/diagnostics/recording-events.tsv");
            var d = new RecordingSessionDiagnostics(log);
            d.started(); d.stopped(142, true, false); d.saveResult(140,"SAVED");
            d.started(); d.stopped(155,false,false); d.saveResult(75,"SHORTENED");
            d.started(); d.stopped(40,false,true); d.saveResult(0,"UNCONFIRMED");
            var lines=Files.readAllLines(log);
            if(lines.size()!=9)throw new AssertionError("Expected 9 session transitions: "+lines.size());
            String[] expected={"START","MANUAL_STOP","SAVED","START","UNEXPECTED_STOP","SHORTENED","START","DISCONNECT","UNCONFIRMED"};
            for(int i=0;i<expected.length;i++)if(!lines.get(i).contains("\t"+expected[i]+"\t"))throw new AssertionError("Wrong event "+i+": "+lines.get(i));
            String all=String.join("\n", lines);
            if(!all.contains("recordedTicks=155; savedTicks=75"))throw new AssertionError("Missing short-capture evidence");
            if(all.contains("server=")||all.contains("chat=")||all.contains("microphone="))throw new AssertionError("Unexpected private content");
            System.out.println("PASS event logs: manual, unexpected, disconnect, short archive and missing save");
        } finally {
            try(var paths=Files.walk(root)) {paths.sorted(java.util.Comparator.reverseOrder()).forEach(path->{try{Files.deleteIfExists(path);}catch(Exception ignored){}});}
        }
    }
}
