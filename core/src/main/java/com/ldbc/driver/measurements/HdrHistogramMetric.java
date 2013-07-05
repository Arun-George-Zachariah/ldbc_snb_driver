package com.ldbc.driver.measurements;

import org.HdrHistogram.Histogram;
import org.apache.log4j.Logger;

import com.ldbc.driver.measurements.formatters.SimpleMetricsFormatter;

public class HdrHistogramMetric implements Metric
{
    private static Logger logger = Logger.getLogger( HdrHistogramMetric.class );

    private final Histogram histogram;
    private final String name;

    public HdrHistogramMetric( long highestExpectedValue )
    {
        this( null, highestExpectedValue );
    }

    public HdrHistogramMetric( String name, long highestExpectedValue )
    {
        this( name, highestExpectedValue, 5 );
    }

    public HdrHistogramMetric( long highestExpectedValue, int numberOfSignificantDigits )
    {
        this( null, highestExpectedValue, numberOfSignificantDigits );
    }

    public HdrHistogramMetric( String name, long highestExpectedValue, int numberOfSignificantDigits )
    {
        histogram = new Histogram( highestExpectedValue, numberOfSignificantDigits );
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void addMeasurement( long value )
    {
        try
        {
            histogram.recordValue( value );
        }
        catch ( ArrayIndexOutOfBoundsException e )
        {
            String errMsg = String.format( "Error encountered adding measurement [%s]", value );
            logger.error( errMsg, e );
            throw new MetricException( errMsg, e.getCause() );
        }
    }

    @Override
    public double getMean()
    {
        return histogram.getHistogramData().getMean();
    }

    @Override
    public long getPercentile( double percentile )
    {
        return histogram.getHistogramData().getValueAtPercentile( percentile );
    }

    @Override
    public long getMin()
    {
        return histogram.getHistogramData().getMinValue();
    }

    @Override
    public long getMax()
    {
        return histogram.getHistogramData().getMaxValue();
    }

    @Override
    public long getCount()
    {
        return histogram.getHistogramData().getTotalCount();
    }

    @Override
    public long getCountAt( long value )
    {
        return histogram.getHistogramData().getCountAtValue( value );
    }

    @Override
    public String toPrettyString()
    {
        return new SimpleMetricsFormatter().format( this );
    }
}
