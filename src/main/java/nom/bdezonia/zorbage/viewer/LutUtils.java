package nom.bdezonia.zorbage.viewer;

import java.io.File;
import java.io.FileInputStream;

import nom.bdezonia.zorbage.type.color.RgbUtils;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class LutUtils {

	public static final int[] DEFAULT_COLOR_TABLE = defaultColorTable();
	
	public static int[] loadLUT(File lutFile) {
		/*
		 * This is my best guess on how to load ImageJ LUT files. My code discovers that
		 * some of them are not stored in triplets so this code is not yet complete.
		 */
		FileInputStream fin = null;
		try {

			if (lutFile.length() > Integer.MAX_VALUE)
				throw new IllegalArgumentException("lut data is too large");

			byte fileContent[] = new byte[(int)lutFile.length()];

			fin = new FileInputStream(lutFile);

			// Reads up to certain bytes of data from this input stream into an array of bytes.
			fin.read(fileContent);

			fin.close();

			// note: some imagej lut files have sizes that are not divisible by 3. this code ignores the couple extra bytes.
			int chunk = fileContent.length / 3;

			int[] lut = new int[chunk];

			for (int i = 0; i < chunk; i++) {
				lut[i] = RgbUtils.argb(0xff, fileContent[0*chunk + i], fileContent[1*chunk + i], fileContent[2*chunk + i]);
			}

			return lut;

		} catch (Exception e) {

			System.out.println("loadLUT exception "+e);

			return DEFAULT_COLOR_TABLE;

		} finally {
			try {
				if (fin != null)
					fin.close();
			} catch (Exception e) {
				;
			}
		}
	}

	private static int[] defaultColorTable() {

		int[] colors = new int[256*3];
		for (int g = 0; g < colors.length; g++) {
			colors[g] = RgbUtils.argb(0xff, g, g, g);
		}
		return colors;
	}
}