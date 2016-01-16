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

import java.beans.*;
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

  private static final Logger LOG = Logger.getLogger( TM1640AnalyserTask.class.getName() );

  // VARIABLES

  private final ToolContext context;
  private final ToolProgressListener progressListener;
  private final AnnotationListener annotationListener;
  private final PropertyChangeSupport pcs;

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

    this.pcs = new PropertyChangeSupport( this );
  }

  // METHODS

  /**
   * Adds the given property change listener.
   * 
   * @param aListener
   *          the listener to add, cannot be <code>null</code>.
   */
  public void addPropertyChangeListener( final PropertyChangeListener aListener )
  {
    this.pcs.addPropertyChangeListener( aListener );
  }

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
    int oldCLK, oldDIN, bitCount;
    int byteValue;

    int startOfDecode = this.context.getStartSampleIndex();
    int endOfDecode = this.context.getEndSampleIndex();

    final int dinMask = ( 1 << this.dinIdx );
    final int clkMask = ( 1 << this.clkIdx );

    final TM1640DataSet tm1640DataSet = new TM1640DataSet( startOfDecode, endOfDecode, data );

    // Prepare everything for the decoding results...
    prepareResults();

    /*
     * Now decode the bytes, SDA may only change when SCL is low. Otherwise it
     * may be a repeated start condition or stop condition. If the start/stop
     * condition is not at a byte boundary a bus error is detected. So we have
     * to scan for SCL rises and for SDA changes during SCL is high. Each byte
     * is followed by a 9th bit (ACK/NACK).
     */
    int idx = tm1640DataSet.getStartOfDecode();
    int prevIdx = -1;

    oldCLK = values[idx] & clkMask;
    oldDIN = values[idx] & dinMask;

    bitCount = 0;
    byteValue = 0;
    
    boolean running = false;

    for ( ; idx < tm1640DataSet.getEndOfDecode(); idx++ )
    {
      final int dataValue = values[idx];

      final int din = ( dataValue & dinMask );
      final int clk = ( dataValue & clkMask );

      if (clk == clkMask && oldDIN == dinMask && din == 0) {
    	  tm1640DataSet.reportStartCondition(  this.dinIdx, idx );
    	  running = true;
		  byteValue = 0;
		  bitCount = 0;
     }

      if (clk == clkMask && oldDIN == 0 && din == dinMask) {
    	  tm1640DataSet.reportStopCondition(  this.dinIdx, idx );
    	  running = false;
      }
      
      if (running) {
    	  if (oldCLK == 0 && clk == clkMask) {
    		  byteValue = (byteValue >> 1) | (din == dinMask ? 0x80 : 0);
    		  if (bitCount == 0) {
    			  prevIdx = idx;
    		  }
        	  bitCount++;
    	  }
    	  if (bitCount == 8) {
    		  tm1640DataSet.reportData(this.dinIdx, prevIdx, idx, byteValue);
    		  String annotation = String.format( "0x%02X %s", byteValue, TM1640DataSet.decodeSegments(byteValue));
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
   * Removes the given property change listener.
   * 
   * @param aListener
   *          the listener to remove, cannot be <code>null</code>.
   */
  public void removePropertyChangeListener( final PropertyChangeListener aListener )
  {
    this.pcs.removePropertyChangeListener( aListener );
  }

  /**
   * @param aLineAmask
   */
  public void setClkIndex( final int clkIdx )
  {
    this.clkIdx = clkIdx;
  }

  /**
   * @param aLineBmask
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

/* EOF */
/*
// detect SCL fall/rise
if ( oldCLK > clk )
{
  // SCL falls
  if ( ( prevIdx < 0 ) || ( bitCount == TM1640_BITCOUNT ) )
  {
    prevIdx = idx;
  }

  if ( bitCount == 0 )
  {
    // store decoded byte
    tm1640DataSet.reportData( this.dinIdx, prevIdx, idx, byteValue );

    final String annotation;
    if ( startCondFound )
    {
      annotation = String.format( "%s data: 0x%X (%c)", "Write",
          Integer.valueOf( byteValue ), Integer.valueOf( byteValue ) );
    } else {
  	  annotation = "OOOPS";
    }

    this.annotationListener.onAnnotation( new SampleDataAnnotation( this.dinIdx, timestamps[prevIdx],
        timestamps[idx], annotation ) );

    byteValue = 0;
  }
}
else if ( clk > oldCLK )
{
  // SCL rises
  if ( din != oldDIN )
  {
  }
  else
  {
    // read SDA
    if ( bitCount != 0 )
    {
      bitCount--;
      if ( din != 0 )
      {
        byteValue |= ( 1 << bitCount );
      }
    }
    else
    {
      // next byte
      bitCount = TM1640_BITCOUNT;
      byteValue = 0;
    }
  }
}

// detect SDA change when SCL high
if ( ( din == dinMask ) && ( din != oldDIN ) )
{
  // SDA changes here
  if ( ( bitCount > 0 ) && ( bitCount < ( TM1640_BITCOUNT - 1 ) ) )
  {
    // bus error, no complete byte detected
  }
  else
  {
    if ( din > oldDIN )
    {
      // SDA rises, this is a stop condition

      this.annotationListener.onAnnotation( new SampleDataAnnotation( this.dinIdx, timestamps[idx],
          TM1640DataSet.TM1640_STOP ) );

    }
    else
    {
      // SDA falls, this is a start condition

      this.annotationListener.onAnnotation( new SampleDataAnnotation( this.dinIdx, timestamps[idx],
          TM1640DataSet.TM1640_START ) );

      startCondFound = true;
    }

    // new byte
    bitCount = TM1640_BITCOUNT;
    byteValue = 0;
  }
}
*/