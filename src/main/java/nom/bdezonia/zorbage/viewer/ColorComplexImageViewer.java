/*
 * zorbage-viewer: utility app for loading and viewing various image data formats
 *
 * Copyright (c) 2020-2022 Barry DeZonia All rights reserved.
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

import nom.bdezonia.zorbage.algebra.AbsoluteValue;
import nom.bdezonia.zorbage.algebra.Addition;
import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.algebra.Allocatable;
import nom.bdezonia.zorbage.algebra.G;
import nom.bdezonia.zorbage.algebra.GetAsDouble;
import nom.bdezonia.zorbage.algebra.GetI;
import nom.bdezonia.zorbage.algebra.GetR;
import nom.bdezonia.zorbage.algebra.InverseTrigonometric;
import nom.bdezonia.zorbage.algebra.Invertible;
import nom.bdezonia.zorbage.algebra.Multiplication;
import nom.bdezonia.zorbage.algebra.NaN;
import nom.bdezonia.zorbage.algebra.Ordered;
import nom.bdezonia.zorbage.algebra.RealConstants;
import nom.bdezonia.zorbage.algebra.Roots;
import nom.bdezonia.zorbage.algebra.Unity;
import nom.bdezonia.zorbage.algorithm.PolarCoords;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.data.DimensionedStorage;
import nom.bdezonia.zorbage.misc.DataSourceUtils;
import nom.bdezonia.zorbage.sampling.IntegerIndex;
import nom.bdezonia.zorbage.sampling.SamplingCartesianIntegerGrid;
import nom.bdezonia.zorbage.sampling.SamplingIterator;
import nom.bdezonia.zorbage.type.color.ArgbAlgebra;
import nom.bdezonia.zorbage.type.color.ArgbMember;

/**
 * 
 * @author Barry DeZonia
 *
 */
public class ColorComplexImageViewer
{
	// do not instantiate
	
	private ColorComplexImageViewer() { }
	
	/**
	 * 
	 * @param <CA>
	 * @param <C>
	 * @param <RA>
	 * @param <R>
	 * @param cAlg
	 * @param rAlg
	 * @param input
	 * @return
	 */
	public static <CA extends Algebra<CA,C>,
					C extends GetR<R> & GetI<R>,
					RA extends Algebra<RA,R> & Roots<R> & Addition<R> &
								Multiplication<R> & Ordered<R> &
								AbsoluteValue<R,R> & Invertible<R> &
								NaN<R> & Unity<R> &
								InverseTrigonometric<R> & RealConstants<R>,
					R extends Allocatable<R> & GetAsDouble>
	
		RgbColorImageViewer<ArgbAlgebra,ArgbMember>
	
			view(Algebra<?,?> cAlg, Algebra<?,?> rAlg, DimensionedDataSource<?> input)
	{
		@SuppressWarnings("unchecked")
		DimensionedDataSource<C> data = (DimensionedDataSource<C>) input;
		
		@SuppressWarnings("unchecked")
		CA complexAlgebra = (CA) cAlg;
		
		@SuppressWarnings("unchecked")
		RA realAlgebra = (RA) rAlg;
		
		R dispMin = realAlgebra.construct("-10000");
		
		R dispMax = realAlgebra.construct("10000");
		
		long[] origDims = DataSourceUtils.dimensions(data);
		
		DimensionedDataSource<ArgbMember> argbData =
				
				DimensionedStorage.allocate(G.ARGB.construct(), origDims);
		
		SamplingCartesianIntegerGrid sampling =
				
				new SamplingCartesianIntegerGrid(origDims);
		
		SamplingIterator<IntegerIndex> iter = sampling.iterator();
		
		IntegerIndex location = new IntegerIndex(origDims.length);

		C complexValue = complexAlgebra.construct();

		R real = realAlgebra.construct();
		
		R imag = realAlgebra.construct();
		
		R magnitude = realAlgebra.construct();
		
		R phase = realAlgebra.construct();
		
		ArgbMember value = G.ARGB.construct();
		
		while (iter.hasNext()) {
			
			iter.next(location);

			data.get(location, complexValue);
			
			complexValue.getR(real);
			
			complexValue.getI(imag);
			
			PolarCoords.magnitude(realAlgebra, real, imag, magnitude);
			
			PolarCoords.phase(realAlgebra, real, imag, phase);

			// from magnitude and phase of complex number calc a HSB
			//   color pixel from the complex number
			//   hue will be from the angle of the phase
			//   saturation will be a fixed constant
			//   brightness will be mapped from the magnitude
			
			double mag = magnitude.getAsDouble();
			
			double dMin = dispMin.getAsDouble();
			
			double dMax = dispMax.getAsDouble();
			
			if (mag < dMin)
				mag = dMin;
			
			if (mag > dMax)
				mag = dMax;
			
			double brightness = (mag - dMin) / (dMax - dMin);
			
			double phas = phase.getAsDouble();
			
			while (phas < 0) 
				phas += (2*Math.PI);
			
			double ang = 360.0 * (phas / (2*Math.PI)); 
			
			double h = ang;
			double s = 0.75;
			double v = brightness;

			// now convert hsv color model to rgb color model
			//   https://www.rapidtables.com/convert/color/hsv-to-rgb.html
			
			double c = v * s;

			double x = c * (1.0 - Math.abs((h / 60.0) % 2.0) - 1);

			double m = v - c;

			final double rPrime;
			final double gPrime;
			final double bPrime;

			if (ang < 60) {
				
				rPrime = c;
				gPrime = x;
				bPrime = 0;
			}
			else if (ang < 120) {
				
				rPrime = x;
				gPrime = c;
				bPrime = 0;
			}
			else if (ang < 180) {
				
				rPrime = 0;
				gPrime = c;
				bPrime = x;
			}
			else if (ang < 240) {
				
				rPrime = 0;
				gPrime = x;
				bPrime = c;
			}
			else if (ang < 300) {
				
				rPrime = x;
				gPrime = 0;
				bPrime = c;
			}
			else {  // ang < 360
				
				rPrime = c;
				gPrime = 0;
				bPrime = x;
			}

			int r = (int) Math.round(255.0 * (rPrime + m));
			int g = (int) Math.round(255.0 * (gPrime + m));
			int b = (int) Math.round(255.0 * (bPrime + m));

			value.setA(225);
			value.setR(r);
			value.setG(g);
			value.setB(b);
			
			argbData.set(location, value);
		}
		
		argbData.setName("Colorization of "+data.getName());

		argbData.setSource(data.getSource());
		
		for (int i = 0; i < argbData.numDimensions(); i++) {

			String type = input.getAxisType(i);
			
			String unit = input.getAxisUnit(i);

			argbData.setAxisType(i, type);
			
			argbData.setAxisUnit(i, unit);
		}

		return new RgbColorImageViewer<>(G.ARGB, argbData);
	}
}
