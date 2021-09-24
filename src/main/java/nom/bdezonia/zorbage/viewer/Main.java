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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Allocatable;
import nom.bdezonia.zorbage.algorithm.Copy;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.data.NdData;
import nom.bdezonia.zorbage.datasource.ConcatenatedDataSource;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.ecat.Ecat;
import nom.bdezonia.zorbage.gdal.Gdal;
import nom.bdezonia.zorbage.misc.DataBundle;
import nom.bdezonia.zorbage.misc.LongUtils;
import nom.bdezonia.zorbage.netcdf.NetCDF;
import nom.bdezonia.zorbage.nifti.Nifti;
import nom.bdezonia.zorbage.scifio.Scifio;
import nom.bdezonia.zorbage.storage.file.FileStorage;
import nom.bdezonia.zorbage.tuple.Tuple2;
import nom.bdezonia.zorbage.type.bool.BooleanMember;
import nom.bdezonia.zorbage.type.character.CharMember;
import nom.bdezonia.zorbage.type.color.ArgbMember;
import nom.bdezonia.zorbage.type.color.CieLabMember;
import nom.bdezonia.zorbage.type.color.RgbMember;
import nom.bdezonia.zorbage.type.complex.float128.ComplexFloat128Member;
import nom.bdezonia.zorbage.type.complex.float128.ComplexFloat128CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.complex.float128.ComplexFloat128MatrixMember;
import nom.bdezonia.zorbage.type.complex.float128.ComplexFloat128VectorMember;
import nom.bdezonia.zorbage.type.complex.float16.ComplexFloat16Member;
import nom.bdezonia.zorbage.type.complex.float16.ComplexFloat16CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.complex.float16.ComplexFloat16MatrixMember;
import nom.bdezonia.zorbage.type.complex.float16.ComplexFloat16VectorMember;
import nom.bdezonia.zorbage.type.complex.float32.ComplexFloat32Member;
import nom.bdezonia.zorbage.type.complex.float32.ComplexFloat32CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.complex.float32.ComplexFloat32MatrixMember;
import nom.bdezonia.zorbage.type.complex.float32.ComplexFloat32VectorMember;
import nom.bdezonia.zorbage.type.complex.float64.ComplexFloat64Member;
import nom.bdezonia.zorbage.type.complex.float64.ComplexFloat64CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.complex.float64.ComplexFloat64MatrixMember;
import nom.bdezonia.zorbage.type.complex.float64.ComplexFloat64VectorMember;
import nom.bdezonia.zorbage.type.complex.highprec.ComplexHighPrecisionMember;
import nom.bdezonia.zorbage.type.complex.highprec.ComplexHighPrecisionCartesianTensorProductMember;
import nom.bdezonia.zorbage.type.complex.highprec.ComplexHighPrecisionMatrixMember;
import nom.bdezonia.zorbage.type.complex.highprec.ComplexHighPrecisionVectorMember;
import nom.bdezonia.zorbage.type.gaussian.int16.GaussianInt16Member;
import nom.bdezonia.zorbage.type.gaussian.int32.GaussianInt32Member;
import nom.bdezonia.zorbage.type.gaussian.int64.GaussianInt64Member;
import nom.bdezonia.zorbage.type.gaussian.int8.GaussianInt8Member;
import nom.bdezonia.zorbage.type.gaussian.unbounded.GaussianIntUnboundedMember;
import nom.bdezonia.zorbage.type.geom.point.Point;
import nom.bdezonia.zorbage.type.octonion.float128.OctonionFloat128Member;
import nom.bdezonia.zorbage.type.octonion.float128.OctonionFloat128CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.octonion.float128.OctonionFloat128MatrixMember;
import nom.bdezonia.zorbage.type.octonion.float128.OctonionFloat128RModuleMember;
import nom.bdezonia.zorbage.type.octonion.float16.OctonionFloat16Member;
import nom.bdezonia.zorbage.type.octonion.float16.OctonionFloat16CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.octonion.float16.OctonionFloat16MatrixMember;
import nom.bdezonia.zorbage.type.octonion.float16.OctonionFloat16RModuleMember;
import nom.bdezonia.zorbage.type.octonion.float32.OctonionFloat32Member;
import nom.bdezonia.zorbage.type.octonion.float32.OctonionFloat32CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.octonion.float32.OctonionFloat32MatrixMember;
import nom.bdezonia.zorbage.type.octonion.float32.OctonionFloat32RModuleMember;
import nom.bdezonia.zorbage.type.octonion.float64.OctonionFloat64Member;
import nom.bdezonia.zorbage.type.octonion.float64.OctonionFloat64CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.octonion.float64.OctonionFloat64MatrixMember;
import nom.bdezonia.zorbage.type.octonion.float64.OctonionFloat64RModuleMember;
import nom.bdezonia.zorbage.type.octonion.highprec.OctonionHighPrecisionMember;
import nom.bdezonia.zorbage.type.octonion.highprec.OctonionHighPrecisionCartesianTensorProductMember;
import nom.bdezonia.zorbage.type.octonion.highprec.OctonionHighPrecisionMatrixMember;
import nom.bdezonia.zorbage.type.octonion.highprec.OctonionHighPrecisionRModuleMember;
import nom.bdezonia.zorbage.type.quaternion.float128.QuaternionFloat128Member;
import nom.bdezonia.zorbage.type.quaternion.float128.QuaternionFloat128CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.quaternion.float128.QuaternionFloat128MatrixMember;
import nom.bdezonia.zorbage.type.quaternion.float128.QuaternionFloat128RModuleMember;
import nom.bdezonia.zorbage.type.quaternion.float16.QuaternionFloat16Member;
import nom.bdezonia.zorbage.type.quaternion.float16.QuaternionFloat16CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.quaternion.float16.QuaternionFloat16MatrixMember;
import nom.bdezonia.zorbage.type.quaternion.float16.QuaternionFloat16RModuleMember;
import nom.bdezonia.zorbage.type.quaternion.float32.QuaternionFloat32Member;
import nom.bdezonia.zorbage.type.quaternion.float32.QuaternionFloat32CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.quaternion.float32.QuaternionFloat32MatrixMember;
import nom.bdezonia.zorbage.type.quaternion.float32.QuaternionFloat32RModuleMember;
import nom.bdezonia.zorbage.type.quaternion.float64.QuaternionFloat64Member;
import nom.bdezonia.zorbage.type.quaternion.float64.QuaternionFloat64CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.quaternion.float64.QuaternionFloat64MatrixMember;
import nom.bdezonia.zorbage.type.quaternion.float64.QuaternionFloat64RModuleMember;
import nom.bdezonia.zorbage.type.quaternion.highprec.QuaternionHighPrecisionMember;
import nom.bdezonia.zorbage.type.quaternion.highprec.QuaternionHighPrecisionCartesianTensorProductMember;
import nom.bdezonia.zorbage.type.quaternion.highprec.QuaternionHighPrecisionMatrixMember;
import nom.bdezonia.zorbage.type.quaternion.highprec.QuaternionHighPrecisionRModuleMember;
import nom.bdezonia.zorbage.type.real.float128.Float128CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.real.float128.Float128MatrixMember;
import nom.bdezonia.zorbage.type.real.float128.Float128VectorMember;
import nom.bdezonia.zorbage.type.real.float16.Float16CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.real.float16.Float16MatrixMember;
import nom.bdezonia.zorbage.type.real.float16.Float16VectorMember;
import nom.bdezonia.zorbage.type.real.float32.Float32CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.real.float32.Float32MatrixMember;
import nom.bdezonia.zorbage.type.real.float32.Float32VectorMember;
import nom.bdezonia.zorbage.type.real.float64.Float64CartesianTensorProductMember;
import nom.bdezonia.zorbage.type.real.float64.Float64MatrixMember;
import nom.bdezonia.zorbage.type.real.float64.Float64VectorMember;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionCartesianTensorProductMember;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionMatrixMember;
import nom.bdezonia.zorbage.type.real.highprec.HighPrecisionVectorMember;
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
	
	public static int GDAL_STATUS = 0;
	
	private static int viewportWidth = 512;
	private static int viewportHeight = 512;

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
 
		JButton loadEcat = new JButton("Load using ecat");
		loadEcat.addMouseListener(new MouseListener() {
			
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

				if (f != null) {
				
					DataBundle bundle = Ecat.loadAllDatasets(f.getAbsolutePath());
				
					displayAll(bundle);
				}
			}
		});

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
						    "GDAL was not found on the system. You must install and/or configure it if you want GDAL functionality in this application.",
						    "WARNING",
						    JOptionPane.WARNING_MESSAGE);
				}
				else {
					JFileChooser chooser = new JFileChooser();
					
					chooser.showOpenDialog(frame);
					
					File f = chooser.getSelectedFile();
	
					if (f != null) {
						
						DataBundle bundle = Gdal.loadAllDatasets(f.getAbsolutePath());
					
						displayAll(bundle);
					}
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

				if (f != null) {
				
					DataBundle bundle = NetCDF.loadAllDatasets(f.getAbsolutePath());
				
					displayAll(bundle);
				}
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

				if (f != null) {
					
					DataBundle bundle = Scifio.loadAllDatasets(f.getAbsolutePath());
				
					displayAll(bundle);
				}
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

				if (f != null) {
				
					long t0 = System.currentTimeMillis();
					
					DataBundle bundle = Nifti.open(f.getAbsolutePath());
				
					long t1 = System.currentTimeMillis();
					
					displayAll(bundle);

					long t2 = System.currentTimeMillis();
					
					System.out.println("LOAD TIME = "+(t1-t0));
					
					System.out.println("DRAW TIME = "+(t2-t1));
				}
			}
		});
		JButton loadVStack = new JButton("Test Nifti VSTACK");
		loadVStack.addMouseListener(new MouseListener() {
			
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
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void mouseClicked(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				
				chooser.setMultiSelectionEnabled(true);  // allow multiple files to be chosen
				
				chooser.showOpenDialog(frame);
				
				File[] files = chooser.getSelectedFiles();
				
				// if user chose some files
				
				if (files != null && files.length != 0) {
					
					// read the first file (presumably into ram) in a DataBundle
					
					DataBundle bundle = Nifti.open(files[0].getAbsolutePath());

					List<Tuple2<T, DimensionedDataSource<U>>> datasources = bundle.bundle();
					
					// if a file was read ...
					
					if (datasources.size() > 0) {
						
						List<IndexedDataSource<U>> sources = new LinkedList<>();

						// gather info about the first source. following sources will use
						//   it as a template.
						
						Tuple2<T, DimensionedDataSource<U>> dataInfo = datasources.get(0);

						T algebra = dataInfo.a();
						
						DimensionedDataSource<U> data = dataInfo.b();
						
						IndexedDataSource<U> fileData = null;
						
						// then gather dims from that first one as a template for others to follow
						
						long[] dims = new long[data.numDimensions()];
						for (int d = 0; d < data.numDimensions(); d++) {
							dims[d] = data.dimension(d);
						}
						
						int i = 0;
						
						do {

							// open the each file one at a time (presumably into ram)
							
							bundle = Nifti.open(files[i].getAbsolutePath());

							data = (DimensionedDataSource<U>) bundle.bundle().get(0).b();
							
							bundle = null; // help out the GC

							// make sure it is a compatible size in comparison to the template data source
							
							if (data.numElements() != LongUtils.numElements(dims)) {
								
								System.out.println("Skipping file "+files[i].getName()+": it does not match dims of file "+files[0].getName());
								i++;
								continue;
							}

							// make a file backed data list with same size as template image's data array
							
							fileData = FileStorage.allocate((Allocatable) algebra.construct(), data.numElements());
						
							// copy the (presumably) ram values to the list that is disk backed
							
							Copy.compute(algebra, data.rawData(), fileData);

							// TODO what about all that glorious metadata? Do I need a copy() algo of some kind?

							// save this populated file data to a list of sources
							
							sources.add(fileData);
							
							data = null; // help the GC know we can get rid of it
							
							i++;
							
						} while (i < files.length);

						if (sources.size() == 1) {
							
							// we got one data source

							// wrap the the result so it is a DimensionedDataSource
							
							NdData<U> ndData = new NdData<>(dims, fileData); 

							displayData(new Tuple2<T,DimensionedDataSource<U>>(algebra, ndData));
						}
						else {

							// we got multiple data sources
							
							// calc a bigger set of dims that represent the concatenated result
							
							long[] newDims = new long[dims.length+1];
							
							for (int d = 0; d < dims.length; d++) {
								newDims[d] = dims[d];
							}
							
							newDims[dims.length] = sources.size();
							
							// concat all the file data lists into one big list
							
							IndexedDataSource<U> concatenatedVirtualDataSource =
									ConcatenatedDataSource.optimalConcat(sources);
							
							// wrap the the result so it is a DimensionedDataSource
							
							NdData<U> ndData =
									new NdData<>(newDims, concatenatedVirtualDataSource); 
							
							// TODO is this where we copy metadata from one of the files?

							// at this point we have a data source that is completely file
							// backed on a per image basis, The mem use is very small. how
							// performant is it? animating. fft. 
							
							displayData(new Tuple2<T,DimensionedDataSource<U>>(algebra, ndData));
						}
					}
				}
			}
		});
		
		Panel bp = new Panel();
		
		Box box = Box.createVerticalBox();
		
		Dimension buttonSize = new Dimension(180,40);
		
		loadEcat.setMinimumSize(buttonSize);
		loadEcat.setMaximumSize(buttonSize);
		loadGdal.setMinimumSize(buttonSize);
		loadGdal.setMaximumSize(buttonSize);
		loadNetcdf.setMinimumSize(buttonSize);
		loadNetcdf.setMaximumSize(buttonSize);
		loadNifti.setMinimumSize(buttonSize);
		loadNifti.setMaximumSize(buttonSize);
		loadScifio.setMinimumSize(buttonSize);
		loadScifio.setMaximumSize(buttonSize);
		loadVStack.setMinimumSize(buttonSize);
		loadVStack.setMaximumSize(buttonSize);
		
		box.add(loadEcat);
		box.add(loadGdal);
		box.add(loadNetcdf);
		box.add(loadNifti);
		box.add(loadScifio);
		box.add(loadVStack);
		
		bp.add(box);

		Container pane = frame.getContentPane();
		
		pane.setLayout(new BorderLayout());
		
		pane.add(bp, BorderLayout.CENTER);
		
		frame.pack();

		frame.setVisible(true);
	}

	private void displayAll(DataBundle bundle) {

		List<Tuple2<T,DimensionedDataSource<U>>> list = bundle.bundle();
		for (int i = 0; i < list.size(); i++) {
			displayData(list.get(i));
		}
	}
	
	private	void displayData(Tuple2<T, DimensionedDataSource<U>> tuple)
	{
		// TODO: there now are all kinds of multidim data sources theoretically possible:
		//   (though no reader yet creates them). For instance:
		//     An n-d data structure containing vectors or matrices or tensors at each point.
		//     All of these can be made of reals, complexes, quaternions, or octonions. What
		//     would be the best way to display this info? Imagine you have no "instanceof"
		//     testing in this render and types do not provide a renderer. Can you use
		//     algebras to reason on how to display resulting data?
		
		U type = tuple.a().construct();
	
		if (type instanceof Point) {
			
			System.out.println("Must display Point based data somehow");
		}
		else if (type instanceof BooleanMember) {
			
			System.out.println("Must display boolean based data somehow");
		}
		else if ((type instanceof FixedStringMember) || (type instanceof StringMember) || (type instanceof CharMember)) {
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
		else if (type instanceof OctonionFloat128CartesianTensorProductMember ||
				type instanceof OctonionFloat64CartesianTensorProductMember ||
				type instanceof OctonionFloat32CartesianTensorProductMember ||
				type instanceof OctonionFloat16CartesianTensorProductMember ||
				type instanceof OctonionHighPrecisionCartesianTensorProductMember)
		{
			
			System.out.println("Must display Octonion tensor based data somehow");
		}	
		else if (type instanceof QuaternionFloat128CartesianTensorProductMember ||
				type instanceof QuaternionFloat64CartesianTensorProductMember ||
				type instanceof QuaternionFloat32CartesianTensorProductMember ||
				type instanceof QuaternionFloat16CartesianTensorProductMember ||
				type instanceof QuaternionHighPrecisionCartesianTensorProductMember)
		{
			
			System.out.println("Must display Quaternion tensor based data somehow");
		}	
		else if (type instanceof ComplexFloat128CartesianTensorProductMember ||
				type instanceof ComplexFloat64CartesianTensorProductMember ||
				type instanceof ComplexFloat32CartesianTensorProductMember ||
				type instanceof ComplexFloat16CartesianTensorProductMember ||
				type instanceof ComplexHighPrecisionCartesianTensorProductMember)
		{
			
			System.out.println("Must display Complex tensor based data somehow");
		}	
		else if (type instanceof Float128CartesianTensorProductMember ||
				type instanceof Float64CartesianTensorProductMember ||
				type instanceof Float32CartesianTensorProductMember ||
				type instanceof Float16CartesianTensorProductMember ||
				type instanceof HighPrecisionCartesianTensorProductMember)
		{
			
			System.out.println("Must display Real tensor based data somehow");
		}	
		else if (type instanceof OctonionFloat128MatrixMember ||
				type instanceof OctonionFloat64MatrixMember ||
				type instanceof OctonionFloat32MatrixMember ||
				type instanceof OctonionFloat16MatrixMember ||
				type instanceof OctonionHighPrecisionMatrixMember)
		{
			
			System.out.println("Must display Octonion matrix based data somehow");
		}	
		else if (type instanceof QuaternionFloat128MatrixMember ||
				type instanceof QuaternionFloat64MatrixMember ||
				type instanceof QuaternionFloat32MatrixMember ||
				type instanceof QuaternionFloat16MatrixMember ||
				type instanceof QuaternionHighPrecisionMatrixMember)
		{
			
			System.out.println("Must display Quaternion matrix based data somehow");
		}	
		else if (type instanceof ComplexFloat128MatrixMember ||
				type instanceof ComplexFloat64MatrixMember ||
				type instanceof ComplexFloat32MatrixMember ||
				type instanceof ComplexFloat16MatrixMember ||
				type instanceof ComplexHighPrecisionMatrixMember)
		{
			
			System.out.println("Must display Complex matrix based data somehow");
		}	
		else if (type instanceof Float128MatrixMember ||
				type instanceof Float64MatrixMember ||
				type instanceof Float32MatrixMember ||
				type instanceof Float16MatrixMember ||
				type instanceof HighPrecisionMatrixMember)
		{
			
			System.out.println("Must display Real matrix based data somehow");
		}	
		else if (type instanceof OctonionFloat128RModuleMember ||
				type instanceof OctonionFloat64RModuleMember ||
				type instanceof OctonionFloat32RModuleMember ||
				type instanceof OctonionFloat16RModuleMember ||
				type instanceof OctonionHighPrecisionRModuleMember)
		{
			
			System.out.println("Must display Octonion rmodule based data somehow");
		}	
		else if (type instanceof QuaternionFloat128RModuleMember ||
				type instanceof QuaternionFloat64RModuleMember ||
				type instanceof QuaternionFloat32RModuleMember ||
				type instanceof QuaternionFloat16RModuleMember ||
				type instanceof QuaternionHighPrecisionRModuleMember)
		{
			
			System.out.println("Must display Quaternion rmodule based data somehow");
		}	
		else if (type instanceof ComplexFloat128VectorMember ||
				type instanceof ComplexFloat64VectorMember ||
				type instanceof ComplexFloat32VectorMember ||
				type instanceof ComplexFloat16VectorMember ||
				type instanceof ComplexHighPrecisionVectorMember)
		{
			
			System.out.println("Must display Complex vector based data somehow");
		}	
		else if (type instanceof Float128VectorMember ||
				type instanceof Float64VectorMember ||
				type instanceof Float32VectorMember ||
				type instanceof Float16VectorMember ||
				type instanceof HighPrecisionVectorMember)
		{
			
			System.out.println("Must display Real vector based data somehow");
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
			// signed and unsigned ints
			// rationals
			// all real floating types
			
			// NOTE: could avoid fall through and test all real types for this
			// section and have a final else clause that says "Can't identify
			// type: ignoring dataset".
			
			displayRealImage(tuple.a(), tuple.b());
		}
	}
	
	private void displayCieLabColorImage(T alg, DimensionedDataSource<U> data) {
		System.out.println("MUST DISPLAY A CIE LAB COLOR IMAGE "+data.getName());
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

	private void displayRgbColorImage(T alg, DimensionedDataSource<U> data) {
		
		new RgbColorImageViewer<T,U>(alg, data);
	}
	
	private	void displayRealImage(T alg, DimensionedDataSource<U> data) {
		
		new RealImageViewer<T,U>(alg, data);
	}
	
}