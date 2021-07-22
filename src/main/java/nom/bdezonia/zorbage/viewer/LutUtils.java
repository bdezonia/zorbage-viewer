/*
 * zorbage-viewer: utility app for loading and viewing various image data formats
 *
 * Copyright (c) 2020-2021 Barry DeZonia All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * Neither the name of the <copyright holder> nor the names of its contributors may
 * be used to endorse or promote products derived from this software without specific
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
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

		int[] colors = new int[256];
		for (int g = 0; g < colors.length; g++) {
			colors[g] = RgbUtils.argb(0xff, g, g, g);
		}
		return colors;
	}
}