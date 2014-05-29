package org.protocjar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Protoc
{
	public static void main(String[] args) {
		try {
			runProtoc(args);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static int runProtoc(String[] args) throws IOException, InterruptedException {
		File protocTemp = extractProtoc();
		
		List<String> protocCmd = new ArrayList<String>();
		protocCmd.add(protocTemp.getAbsolutePath());
		protocCmd.addAll(Arrays.asList(args));
		ProcessBuilder pb = new ProcessBuilder(protocCmd);
		log("executing: " + protocCmd);
		
		Process protoc = pb.start();
		new Thread(new StreamCopier(protoc.getInputStream(), System.out)).start();
		new Thread(new StreamCopier(protoc.getErrorStream(), System.err)).start();
		int exitCode = protoc.waitFor();
		
		if (protocTemp != null) protocTemp.delete();
		return exitCode;
	}

	static File extractProtoc() throws IOException {
		String protocStr = "protoc";
		String resourcePath = null; // for jar
		String filePath = null; // for test

		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.startsWith("win")) {
			resourcePath = "/bin/win32/" + protocStr + ".exe";
			filePath = sProtocFilePath + "/win32/" + protocStr + ".exe";
		}
		else if (osName.startsWith("linux")) {
			resourcePath = "/bin/linux/";
			filePath = sProtocFilePath + "/linux/";
		}
		else if (osName.startsWith("mac")) {
			resourcePath = "/bin/mac/";
			filePath = sProtocFilePath + "/mac/";
		}
		else {
			throw new IOException("Unsupported platform: " + osName);
		}
		
		Class<Protoc> clazz = Protoc.class;
		InputStream is = clazz.getResourceAsStream(resourcePath);
		if (is == null) is = new FileInputStream(filePath);
		
		File temp = File.createTempFile(protocStr, ".exe");
		temp.setExecutable(true);
		FileOutputStream os = new FileOutputStream(temp);
		streamCopy(is, os);
		is.close();
		os.close();
		return temp;
	}

	static void streamCopy(InputStream in, OutputStream out) throws IOException {
		int read = 0;
		byte[] buf = new byte[4096];
		while ((read = in.read(buf)) > 0) out.write(buf, 0, read);		
	}

	static void log(String msg) {
		System.out.println("protoc-jar: " + msg);
	}

	static class StreamCopier implements Runnable
	{
		public StreamCopier(InputStream in, OutputStream out) {
			mIn = in;
			mOut = out;
		}

		public void run() {
			try {
				streamCopy(mIn, mOut);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		private InputStream mIn;
		private OutputStream mOut;
	}

	static String sProtocFilePath = "bin_241";
}
