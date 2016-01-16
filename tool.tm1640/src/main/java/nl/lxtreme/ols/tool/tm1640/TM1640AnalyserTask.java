/*
 * OpenBench LogicSniffer / SUMP project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * 
 * Copyright (C) 2010-2011 - J.W. Janssen, http://www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.tm1640;


import static nl.lxtreme.ols.util.NumberUtils.*;

import java.util.logging.*;

import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.annotation.AnnotationListener;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.tool.base.annotation.*;


/**
 * Performs the actual TM1640 analysis.
 */
public class TM1640AnalyserTask implements ToolTask<TM1640DataSet>
{
  // CONSTANTS

  private static final String CHANNEL_CLK_NAME = "CLK";
  private static final String CHANNEL_DIN_NAME = "DIN";

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger( TM1640AnalyserTask.class.getName() );

  // VARIABLES

  private final ToolContext context;
  private final ToolProgressListener progressListener;
  private final AnnotationListener annotationListener;

  private int dinIdx;
  private int clkIdx;

  // CONSTRUCTORS

  /**
   * Creates a new TM1640AnalyserTask instance.
   * 
   * @param aContext
   * @param aProgressListener
   */
  public TM1640AnalyserTask( final ToolContext aContext, final ToolProgressListener aProgressListener,
      final AnnotationListener aAnnotationListener )
  {
    this.context = aContext;
    this.progressListener = aProgressListener;
    this.annotationListener = aAnnotationListener;
  }

  // METHODS

  /**
   * This is the TM1640 protocol decoder core The decoder scans for a decode start
   * event when one of the two lines is going low (start condition). After this
   * the decoder starts to decode the data.
   * 
   * @see javax.swing.SwingWorker#doInBackground()
   */
  @Override
  public TM1640DataSet call() throws Exception
  {
    final AcquisitionResult data = this.context.getData();

    final int[] values = data.getValues();
    final long[] timestamps = data.getTimestamps();

    // process the captured data and write to output
    boolean oldCLK, oldDIN;
    int bitCount;
    int byteValue;

    int startOfDecode = this.context.getStartSampleIndex();
    int endOfDecode = this.context.getEndSampleIndex();

    final int clkMask = ( 1 << this.clkIdx );
    final int dinMask = ( 1 << this.dinIdx );

    final TM1640DataSet tm1640DataSet = new TM1640DataSet( startOfDecode, endOfDecode, data );

    // Prepare everything for the decoding results...
    prepareResults();

    int idx = tm1640DataSet.getStartOfDecode();
    int prevIdx = -1;

    oldCLK = (values[idx] & clkMask) == clkMask;
    oldDIN = (values[idx] & dinMask) == dinMask;

    bitCount = 0;
    byteValue = 0;
    
    boolean running = false;

    for ( ; idx < tm1640DataSet.getEndOfDecode(); idx++ )
    {
      final int dataValue = values[idx];

      final boolean clk = ( dataValue & clkMask ) == clkMask;
      final boolean din = ( dataValue & dinMask ) == dinMask;

      if (clk && oldDIN && !din) {
    	  // START: CLK high, falling edge on DIN.
    	  tm1640DataSet.reportStartCondition(  this.dinIdx, idx );
    	  running = true;
		  byteValue = 0;
		  bitCount = 0;
     }

      if (clk && !oldDIN && din) {
    	  // STOP: CLK high, rising edge on DIN.
    	  tm1640DataSet.reportStopCondition(  this.dinIdx, idx );
    	  running = false;
      }
      
      if (running) {
    	  if (!oldCLK && clk) {
    		  // BIT: Rising edge on CLK.
    		  byteValue = (byteValue >> 1) | (din ? 0x80 : 0);
    		  if (bitCount == 0) {
    			  prevIdx = idx;
    		  }
        	  bitCount++;
    	  }
    	  if (bitCount == 8) {
    		  tm1640DataSet.reportData(this.dinIdx, prevIdx, idx, byteValue);
    		  final String annotation = String.format( "0x%02X %s", byteValue, TM1640DataSet.decodeSegments(byteValue));
        	  this.annotationListener.onAnnotation( new SampleDataAnnotation( this.dinIdx, timestamps[prevIdx],
        			  timestamps[idx], annotation ) );
    		  byteValue = 0;
    		  bitCount = 0;
    	  }
      }

      oldCLK = clk;
      oldDIN = din;
      
      this.progressListener
          .setProgress( getPercentage( idx, tm1640DataSet.getStartOfDecode(), tm1640DataSet.getEndOfDecode() ) );
    }

    return tm1640DataSet;
  }

  /**
   * @param clkIdx
   */
  public void setClkIndex( final int clkIdx )
  {
    this.clkIdx = clkIdx;
  }

  /**
   * @param dinIdx
   */
  public void setDinIndex( final int dinIdx )
  {
    this.dinIdx = dinIdx;
  }

  /**
   * @return the clkIdx
   */
  final int getClkIdx()
  {
    return this.clkIdx;
  }

  /**
   * @return the dinIdx
   */
  final int getDinIdx()
  {
    return this.dinIdx;
  }

  /**
   * Prepares everything for the upcoming results.
   */
  private void prepareResults()
  {
    // Update the channel labels...
    this.annotationListener.onAnnotation( new ChannelLabelAnnotation( this.clkIdx, CHANNEL_CLK_NAME ) );
    this.annotationListener.clearAnnotations( this.clkIdx );

    this.annotationListener.onAnnotation( new ChannelLabelAnnotation( this.dinIdx, CHANNEL_DIN_NAME ) );
    this.annotationListener.clearAnnotations( this.dinIdx );
  }

}