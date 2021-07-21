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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algorithm.GridIterator;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.gdal.Gdal;
import nom.bdezonia.zorbage.misc.DataBundle;
import nom.bdezonia.zorbage.netcdf.NetCDF;
import nom.bdezonia.zorbage.nifti.Nifti;
import nom.bdezonia.zorbage.sampling.IntegerIndex;
import nom.bdezonia.zorbage.sampling.SamplingIterator;
import nom.bdezonia.zorbage.scifio.Scifio;
import nom.bdezonia.zorbage.tuple.Tuple2;
import nom.bdezonia.zorbage.type.character.CharMember;
import nom.bdezonia.zorbage.type.color.ArgbMember;
import nom.bdezonia.zorbage.type.color.CieLabMember;
import nom.bdezonia.zorbage.type.color.RgbMember;
import nom.bdezonia.zorbage.type.color.RgbUtils;
import nom.bdezonia.zorbage.type.complex.float128.ComplexFloat128Member;
import nom.bdezonia.zorbage.type.complex.float16.ComplexFloat16Member;
import nom.bdezonia.zorbage.type.complex.float32.ComplexFloat32Member;
import nom.bdezonia.zorbage.type.complex.float64.ComplexFloat64Member;
import nom.bdezonia.zorbage.type.complex.highprec.ComplexHighPrecisionMember;
import nom.bdezonia.zorbage.type.gaussian.int16.GaussianInt16Member;
import nom.bdezonia.zorbage.type.gaussian.int32.GaussianInt32Member;
import nom.bdezonia.zorbage.type.gaussian.int64.GaussianInt64Member;
import nom.bdezonia.zorbage.type.gaussian.int8.GaussianInt8Member;
import nom.bdezonia.zorbage.type.gaussian.unbounded.GaussianIntUnboundedMember;
import nom.bdezonia.zorbage.type.octonion.float128.OctonionFloat128Member;
import nom.bdezonia.zorbage.type.octonion.float16.OctonionFloat16Member;
import nom.bdezonia.zorbage.type.octonion.float32.OctonionFloat32Member;
import nom.bdezonia.zorbage.type.octonion.float64.OctonionFloat64Member;
import nom.bdezonia.zorbage.type.octonion.highprec.OctonionHighPrecisionMember;
import nom.bdezonia.zorbage.type.quaternion.float128.QuaternionFloat128Member;
import nom.bdezonia.zorbage.type.quaternion.float16.QuaternionFloat16Member;
import nom.bdezonia.zorbage.type.quaternion.float32.QuaternionFloat32Member;
import nom.bdezonia.zorbage.type.quaternion.float64.QuaternionFloat64Member;
import nom.bdezonia.zorbage.type.quaternion.highprec.QuaternionHighPrecisionMember;
import nom.bdezonia.zorbage.type.string.FixedStringMember;
import nom.bdezonia.zorbage.type.string.StringMember;

// TODO
//
//   I should do a little rewriting. A dataset can be a plane or multidims. Each
//   element can be a number or a vector or a matrix or a tensor. Each of those
//   can be int/real/complex/quaternion/octonion. Need to think of best way to
//   detect these differences and to then write specialized display routines to
//   handle all these differences. Zorbage types might need some identifying info
//   embedded in their types.

/**
 * 
 * @author Barry DeZonia
 *
 */
public class Main<T extends Algebra<T,U>, U> {

	private JFrame frame = null;
	
	private List<Tuple2<T,DimensionedDataSource<U>>> dataSources = new ArrayList<>();
	
	private int dsNumber = 0;

	private Component image = null;
	
	public static int[] DEFAULT_COLOR_TABLE = defaultColorTable();

	public static int GDAL_STATUS = 0;

	private int[] colorTable = DEFAULT_COLOR_TABLE;

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {

		GDAL_STATUS = Gdal.init();

		Main main = new Main();
		
		// Schedule a job for the event-dispatching thread: creating and showing
		// this application's GUI.
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				main.createAndShowGUI();
			}
		});
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private void createAndShowGUI() {

		//Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);
 
		//Create and set up the window.
		frame = new JFrame("Zorbage Data Viewer");
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
		JButton loadGdal = new JButton("Load using gdal");
		loadGdal.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {

				if (GDAL_STATUS == 0) {
				JFileChooser chooser = new JFileChooser();
				
				chooser.showOpenDialog(frame);
				
				File f = chooser.getSelectedFile();

				DataBundle bundle = Gdal.loadAllDatasets(f.getAbsolutePath());
	
				int nextDs = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				displayData(dataSources.get(nextDs));
				
				dsNumber = nextDs;
				}
				else {  // GDAL was not foud on system
					JOptionPane.showMessageDialog(frame,
						    "GDAL was not found on the system. Please install it.",
						    "WARNING",
						    JOptionPane.WARNING_MESSAGE);
				}
			}
		});

		JButton loadNetcdf = new JButton("Load using netcdf");
		loadNetcdf.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				
				chooser.showOpenDialog(frame);
				
				File f = chooser.getSelectedFile();

				DataBundle bundle = NetCDF.loadAllDatasets(f.getAbsolutePath());
	
				int nextDs = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				displayData(dataSources.get(nextDs));
				
				dsNumber = nextDs;
			}
		});

		JButton loadScifio = new JButton("Load using scifio");
		loadScifio.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				
				chooser.showOpenDialog(frame);
				
				File f = chooser.getSelectedFile();

				DataBundle bundle = Scifio.loadAllDatasets(f.getAbsolutePath());
	
				int nextDs = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				displayData(dataSources.get(nextDs));
				
				dsNumber = nextDs;
			}
		});

		JButton loadNifti = new JButton("Load using nifti");
		loadNifti.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				
				chooser.showOpenDialog(frame);
				
				File f = chooser.getSelectedFile();

				DataBundle bundle = Nifti.open(f.getAbsolutePath());
	
				int nextDs = dataSources.size();
				
				dataSources.addAll(bundle.bundle());
				
				displayData(dataSources.get(nextDs));
				
				dsNumber = nextDs;
			}
		});

		JButton displayNext = new JButton("Display Next");
		displayNext.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (dsNumber+1 >= dataSources.size())
					java.awt.Toolkit.getDefaultToolkit().beep();
				else {
					dsNumber++;
					displayData(dataSources.get(dsNumber));
				}
			}
		});

		JButton displayPrev = new JButton("Display Prev");
		displayPrev.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (dsNumber-1 < 0)
					java.awt.Toolkit.getDefaultToolkit().beep();
				else {
					dsNumber--;
					displayData(dataSources.get(dsNumber));
				}
			}
		});

		JButton loadLut = new JButton("Choose LUT");
		loadLut.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				JFileChooser dlg = new JFileChooser();
				int returnVal = dlg.showDialog(frame, "OK");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File lutFile = dlg.getSelectedFile();
					colorTable = loadLUT(lutFile);
					if (dsNumber >= 0 && dsNumber < dataSources.size()) {
						displayData(dataSources.get(dsNumber));
					}
				}
			}
		});

		JButton resetLut = new JButton("Reset LUT");
		resetLut.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				colorTable = DEFAULT_COLOR_TABLE;
				if (dsNumber >= 0 && dsNumber < dataSources.size()) {
					displayData(dataSources.get(dsNumber));
				}
			}
		});

		BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		
		image = new JLabel(new ImageIcon(img));
				
		Panel bp = new Panel();
		
		bp.add(loadGdal);
		bp.add(loadNetcdf);
		bp.add(loadNifti);
		bp.add(loadScifio);

		Panel ip = new Panel();
		
		ip.add(displayPrev);
		ip.add(displayNext);
		ip.add(loadLut);
		ip.add(resetLut);
		
		Container pane = frame.getContentPane();
		
		pane.setLayout(new BorderLayout());
		
		pane.add(bp, BorderLayout.NORTH);
		pane.add(ip, BorderLayout.SOUTH);
		pane.add(image, BorderLayout.CENTER);
		
		frame.pack();

		frame.setVisible(true);
	}

	private	void displayData(Tuple2<T, DimensionedDataSource<U>> tuple)
	{
		// TODO: there now are all kinds of multidim data sources theoretically possible:
		//   (though no reader yet creates them). For instance:
		//     An n-d data structure containing vectors or matrices or tensors at each point.
		//     All of these can be made of reals, complexes, quaternions, or octonions. What
		//     would be the best way to display this info?
		
		U type = tuple.a().construct();
		
		if ((type instanceof FixedStringMember) || (type instanceof StringMember) || (type instanceof CharMember)) {
			displayTextData(tuple.a(), tuple.b());
		}
		else if (type instanceof CieLabMember)
		{
			displayCieLabColorImage(tuple.a(), tuple.b());
		}
		else if ((type instanceof RgbMember) || (type instanceof ArgbMember))
		{
			int xId = 0;
			int yId = 1;
			int numExtraDims = tuple.b().numDimensions()-2;
			if (numExtraDims < 0)
				numExtraDims = 0;
			long[] otherDimVals = new long[numExtraDims];
			displayRgbColorImage(tuple.a(), tuple.b(), xId, yId, otherDimVals);
		}
		else if ((type instanceof OctonionFloat16Member) ||
				(type instanceof OctonionFloat32Member) ||
				(type instanceof OctonionFloat64Member) ||
				(type instanceof OctonionFloat128Member) ||
				(type instanceof OctonionHighPrecisionMember))
		{
			displayOctonionImage(tuple.a(), tuple.b());
		}
		else if ((type instanceof QuaternionFloat16Member) ||
				(type instanceof QuaternionFloat32Member) ||
				(type instanceof QuaternionFloat64Member) ||
				(type instanceof QuaternionFloat128Member) ||
				(type instanceof QuaternionHighPrecisionMember))
		{
			displayQuaternionImage(tuple.a(), tuple.b());
		}
		else if ((type instanceof ComplexFloat16Member) ||
				(type instanceof ComplexFloat32Member) ||
				(type instanceof ComplexFloat64Member) ||
				(type instanceof ComplexFloat128Member) ||
				(type instanceof ComplexHighPrecisionMember) ||
				(type instanceof GaussianInt8Member) ||
				(type instanceof GaussianInt16Member) ||
				(type instanceof GaussianInt32Member) ||
				(type instanceof GaussianInt64Member) ||
				(type instanceof GaussianIntUnboundedMember))
		{
			displayComplexImage(tuple.a(), tuple.b());
		}
		else {
			int xId = 0;
			int yId = 1;
			int numExtraDims = tuple.b().numDimensions()-2;
			if (numExtraDims < 0)
				numExtraDims = 0;
			long[] otherDimVals = new long[numExtraDims];
			displayRealImage(tuple.a(), tuple.b(), xId, yId, otherDimVals);
		}
	}
	
	private void displayCieLabColorImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A CIE LAB COLOR IMAGE "+data.getName());
	}

	private void displayRgbColorImage(T alg, DimensionedDataSource<U> data, int xId, int yId, long[] otherDimVals) {
		int numD = data.numDimensions();
		if (numD < 1)
			throw new IllegalArgumentException("dataset is completely void: nothing to display");
		U value = alg.construct();
		long width  = xId == -1 ? 1 : data.dimension(xId);
		long height = yId == -1 ? 1 : data.dimension(yId);
		IntegerIndex idx = new IntegerIndex(numD);
		long[] minPt = new long[numD];
		long[] maxPt = new long[numD];
		if (xId != -1) {
			minPt[xId] = 0;
			maxPt[xId] = data.dimension(xId) - 1;
		}
		if (yId != -1) {
			minPt[yId] = 0;
			maxPt[yId] = data.dimension(yId) - 1;
		}
		int dimNumber = 0;
		for (int i = 0; i < numD; i++) {
			if (i != xId && i != yId) {
				minPt[i] = otherDimVals[dimNumber];
				maxPt[i] = otherDimVals[dimNumber];
				dimNumber++;
			}
		}
		
		BufferedImage img = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
		
		// Safe cast as img is of correct type 
		
		DataBufferInt buffer = (DataBufferInt) img.getRaster().getDataBuffer();

		// Conveniently, the buffer already contains the data array
		int[] arrayInt = buffer.getData();

		SamplingIterator<IntegerIndex> iter = GridIterator.compute(minPt, maxPt);
		while (iter.hasNext()) {
			iter.next(idx);
			data.get(idx, value);
			int v;
			if (value instanceof ArgbMember) {
				ArgbMember tmp = (ArgbMember) value;
				v = RgbUtils.argb(tmp.a(), tmp.r(), tmp.g(), tmp.b());
			}
			else if (value instanceof RgbMember) {
				RgbMember tmp = (RgbMember) value;
				v = RgbUtils.argb(255, tmp.r(), tmp.g(), tmp.b());
			}
			else
				throw new IllegalArgumentException("Unsupported color type passed to display routine");

			long x = xId == -1 ? 0 : idx.get(xId);
			long y = yId == -1 ? 0 : idx.get(yId);
			
			int bufferPos = (int) (y * width + x);
			
			arrayInt[bufferPos] = v;
		}
		
		Container pane = frame.getContentPane();
		
		pane.remove(image);
		
		image = new JLabel(new ImageIcon(img));
		
		image = new JScrollPane(image);
		
		pane.add(image, BorderLayout.CENTER);

		frame.setTitle("Zorbage Data Viewer - " + data.getName());
		
		frame.pack();

		frame.repaint();
	}
	
	// how would you display complex data? a channel for r and i? otherwise it's not
	//   bounded and it's not ordered so you can't get a display range from it. Also
	//   similar things for quat and oct images
	
	private void displayComplexImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A COMPLEX IMAGE "+data.getName());
	}

	private void displayQuaternionImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A QUATERNION IMAGE "+data.getName());
	}

	private void displayOctonionImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY AN OCTONION IMAGE "+data.getName());
	}

	@SuppressWarnings("unchecked")
	private void displayTextData(T alg, DimensionedDataSource<U> data) {

		Container pane = frame.getContentPane();
		
		pane.remove(image);
		
		image = new JTextPane();

		U type = alg.construct();
		
		String result = "";

		if (type instanceof StringMember) {
			StringMember value = new StringMember();
			IndexedDataSource<StringMember> stringList = (IndexedDataSource<StringMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(0, value);
				result = result + "\n" + value.toString();
			}
		}
		else if (type instanceof FixedStringMember) {
			FixedStringMember value = new FixedStringMember(256);
			IndexedDataSource<FixedStringMember> stringList = (IndexedDataSource<FixedStringMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(0, value);
				result = result + "\n" + value.toString();
			}
		}
		else if (type instanceof CharMember) {
			CharMember value = new CharMember();
			IndexedDataSource<CharMember> stringList = (IndexedDataSource<CharMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(0, value);
				result = result + value.toString();
			}
		}
		else
			throw new IllegalArgumentException("Unknown string type "+type);
		
		((JTextPane)image).setText(result);
		
		image = new JScrollPane(image);
		
		pane.add(image, BorderLayout.CENTER);

		frame.setTitle("Zorbage Data Viewer - " + data.getName());
		
		frame.pack();

		frame.repaint();
	}

	private	void displayRealImage(T alg, DimensionedDataSource<U> data, int xId, int yId, long[] otherDimVals)
	{
		new RealImageViewer<T,U>(alg, data);
	}
	
	private static int[] defaultColorTable() {

		int[] colors = new int[256*3];
		int r = 0, g = 0, b = 0;
		for (int i = 0; i < colors.length; i++) {
			colors[i] = RgbUtils.argb(0xff, r, g, b);
			if (i % 3 == 0) {
				b++;
			}
			else if (i % 3 == 1) {
				r++;
			}
			else {
				g++;
			}
		}
		return colors;
	}
	
	private int[] loadLUT(File lutFile) {
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

			return colorTable;

		} finally {
			try {
				if (fin != null)
					fin.close();
			} catch (Exception e) {
				;
			}
		}
	}
}