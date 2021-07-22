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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.gdal.Gdal;
import nom.bdezonia.zorbage.misc.DataBundle;
import nom.bdezonia.zorbage.netcdf.NetCDF;
import nom.bdezonia.zorbage.nifti.Nifti;
import nom.bdezonia.zorbage.scifio.Scifio;
import nom.bdezonia.zorbage.tuple.Tuple2;
import nom.bdezonia.zorbage.type.character.CharMember;
import nom.bdezonia.zorbage.type.color.ArgbMember;
import nom.bdezonia.zorbage.type.color.CieLabMember;
import nom.bdezonia.zorbage.type.color.RgbMember;
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
	
	public static int GDAL_STATUS = 0;

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

				// GDAL was not found on system or failed to init?
				if (GDAL_STATUS != 0) {
					JOptionPane.showMessageDialog(frame,
						    "GDAL was not found on the system. Please install it.",
						    "WARNING",
						    JOptionPane.WARNING_MESSAGE);
				}
				else {
					JFileChooser chooser = new JFileChooser();
					
					chooser.showOpenDialog(frame);
					
					File f = chooser.getSelectedFile();
	
					DataBundle bundle = Gdal.loadAllDatasets(f.getAbsolutePath());
		
					int nextDs = dataSources.size();
					
					dataSources.addAll(bundle.bundle());
					
					displayData(dataSources.get(nextDs));
					
					dsNumber = nextDs;
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
			displayRgbColorImage(tuple.a(), tuple.b());
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
			displayRealImage(tuple.a(), tuple.b());
		}
	}
	
	private void displayCieLabColorImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A CIE LAB COLOR IMAGE "+data.getName());
	}

	private void displayRgbColorImage(T alg, DimensionedDataSource<U> data) {
		
		new RgbColorImageViewer<T,U>(alg, data);
	}
	
	// how would you display complex data? a channel for r and i? otherwise it's not
	//   bounded and it's not ordered so you can't get a display range from it. Also
	//   similar things for quat and oct images. Maybe one window per "channel" and
	//   you can get bounds on a channel.
	
	private void displayComplexImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A COMPLEX IMAGE "+data.getName());
	}

	private void displayQuaternionImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A QUATERNION IMAGE "+data.getName());
	}

	private void displayOctonionImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY AN OCTONION IMAGE "+data.getName());
	}

	private void displayTextData(T alg, DimensionedDataSource<U> data) {

		new TextViewer<T,U>(alg, data);
	}

	private	void displayRealImage(T alg, DimensionedDataSource<U> data) {
		
		new RealImageViewer<T,U>(alg, data);
	}
	
}