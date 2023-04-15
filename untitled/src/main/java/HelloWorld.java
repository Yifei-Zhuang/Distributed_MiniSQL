import java.io.*;

public class HelloWorld {
    public static void command(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.directory(new File("/Users/zhuangyifei/Downloads/Distributed_MiniSQL/untitled/"));
        Process p = pb.start();
        OutputStream stdin = p.getOutputStream();
//        InputStream stdout = p.getInputStream();
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(stdin));

// Write to the input stream of the process
        printWriter.println("aha");
        printWriter.flush();

// Read from the output stream of the process
//        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        String line;
//        while ((line = reader.readLine()) != null) {
//            System.out.println(line);
//        }
        printWriter.println("aha");
//        while ((line = reader.readLine()) != null) {
//            System.out.println(line);
//        }
        printWriter.println("aha");
//        while ((line = reader.readLine()) != null) {
//            System.out.println(line);
//        }
    }

    public static void main(String[] args) throws IOException {
        command("./res");
    }
}