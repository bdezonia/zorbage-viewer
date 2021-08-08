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
import java.awt.Dimension;
import java.io.StringReader;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import nom.bdezonia.zorbage.algebra.Algebra;
import nom.bdezonia.zorbage.data.DimensionedDataSource;
import nom.bdezonia.zorbage.datasource.IndexedDataSource;
import nom.bdezonia.zorbage.type.character.CharMember;
import nom.bdezonia.zorbage.type.string.FixedStringMember;
import nom.bdezonia.zorbage.type.string.StringMember;

/**
 * 
 * @author Barry DeZonia
 *
 * @param <T>
 * @param <U>
 */
public class TextViewer<T extends Algebra<T, U>,U> {

	private final JFrame frame;
	
	@SuppressWarnings("unchecked")
	public TextViewer(T alg, DimensionedDataSource<U> data) {

		frame = new JFrame();
		
		frame.setLocationByPlatform(true);
		
		Container pane = frame.getContentPane();
		
		Component image = new JTextPane();

		U type = alg.construct();
		
		StringBuilder result = new StringBuilder();

		if (type instanceof StringMember) {
			StringMember value = new StringMember();
			IndexedDataSource<StringMember> stringList = (IndexedDataSource<StringMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(i, value);
				if (i != 0)
					result.append('\n');
				result.append(value.toString());
			}
		}
		else if (type instanceof FixedStringMember) {
			FixedStringMember value = new FixedStringMember(256);
			IndexedDataSource<FixedStringMember> stringList = (IndexedDataSource<FixedStringMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(i, value);
				if (i != 0)
					result.append('\n');
				result.append(value.toString());
			}
		}
		else if (type instanceof CharMember) {
			CharMember value = new CharMember();
			IndexedDataSource<CharMember> stringList = (IndexedDataSource<CharMember>) data.rawData();
			for (long i = 0; i < stringList.size(); i++) {
				stringList.get(i, value);
				if (i != 0)
					result.append('\n');
				result.append(value.toString());
			}
		}
		else
			throw new IllegalArgumentException("Unknown string type "+type);
		
		try {
			((JTextPane)image).read(new StringReader(result.toString()), "text file");
		} catch (Exception e) {
			System.out.println("Could not read txt");
		}
		
		image = new JScrollPane(image);
		
		pane.add(image, BorderLayout.CENTER);

		String dsName = data.getName() == null ? "<unknown>" : data.getName();
		
		frame.setPreferredSize(new Dimension(512, 512));
		
		frame.setTitle("Zorbage Data Viewer - " + dsName);
		
		frame.pack();

		frame.setVisible(true);

		//frame.repaint();
	}
}
