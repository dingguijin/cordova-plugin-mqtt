import java.io.File;

public class PluginBuilder {

	private static void build(File file, String filePrefix, String dirPrefix) {
		File[] files = file.listFiles();
		for (int i = 0; i < files.length; ++i) {
			if (files[i].isDirectory()) {
				String dirName = files[i].getName().equals("android") ? "" : "/" + files[i].getName();
				build(files[i], filePrefix + "/" + files[i].getName(),
						dirPrefix + dirName);
			} else {
                if (files[i].getName().endsWith(".java") || files[i].getName().endsWith(".properties")) {
				    print(filePrefix + "/" + files[i].getName(), dirPrefix);
                }
			}
		}
	}
	
	private static void print(String file, String dir) {
		System.out.println(String.format("<source-file src=\"%s\" target-dir=\"%s\" />", file, dir));
	}
	
	public static void main(String args[]) {
        //replace this by your plugin's code path
        String path = "/Users/dingguijin/Documents/cordova-plugin-mqtt/src";
		build(new File(path), "src", "src");
	}
}
