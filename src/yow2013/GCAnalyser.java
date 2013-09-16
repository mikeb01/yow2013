package yow2013;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCAnalyser
{
    static final Pattern GC_PATTERN = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3})\\+[0-9]{4}: .*");
    static final Pattern STOPPED_PATTERN = Pattern.compile("Total time for which application threads were stopped: ([0-9.]+) seconds");
    static final SimpleDateFormat DATESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    static final String GNUPLOT_PAUSE_COMMAND =
            "set terminal png size 1680,1050%n" +
            "set output \"%s\"%n" +
            "set xdata time%n" +
            "set timefmt \"%%s\"%n" +
            "set format x \"%%H:%%M:%%S\"%n" +
            "set log y%n" +
            "set log y2%n" +
            "set y2tics border%n" +
            "set grid%n" +
            "plot \"%s\" using 1:2 title \"Total Pause Time\" lt -1 pt 3," +
            " \"%s\" using 1:3 title \"Max Pause Time\" lt 3 pt 7," +
            " \"%s\" using 1:2 axes x1y2 title \"Portion of Time in Pause\" lt 1 pt 1%n";

    public static void main(final String[] args) throws IOException, ParseException
    {
        if (args.length < 2)
        {
            System.err.println("Usage: GCAnalyser <filename> <output> [<start timestamp>]");
            return;
        }

        final String inputFilename = args[0];
        final String outputName = args[1];
        final long startTimestamp = args.length > 2 ? Long.parseLong(args[2]) : 0L;

        final String pauseFilename = outputName + "-pause.csv";
        final String percentFilename = outputName + "-percent.csv";
        final String plotFilename = outputName + ".plot";
        final String imageFilename = outputName + "-gc.png";

        final BufferedReader gcLogReader = new BufferedReader(new FileReader(inputFilename));
        final PrintWriter pauseWriter = new PrintWriter(pauseFilename);
        final PrintWriter percentWriter = new PrintWriter(percentFilename);
        final Date monitoringStartTimestamp = new Date(startTimestamp);

        runAnalysis(gcLogReader, pauseWriter, percentWriter, monitoringStartTimestamp);

        closeFile(pauseFilename, pauseWriter);
        closeFile(percentFilename, percentWriter);

        printGnuPlotCommand(plotFilename, GNUPLOT_PAUSE_COMMAND, imageFilename, pauseFilename, percentFilename);
    }

    private static void runAnalysis(final BufferedReader gcLogReader, final PrintWriter pauseWriter, final PrintWriter percentWriter,
                                    final Date monitoringStartTimestamp) throws IOException, ParseException
    {
        Date currentDateTime = null;
        double totalTime = 0;
        double maxTime = 0;

        String line;
        while ((line = gcLogReader.readLine()) != null)
        {
            final Matcher stoppedMatcher = STOPPED_PATTERN.matcher(line);
            if (stoppedMatcher.matches())
            {
                if (null != currentDateTime)
                {
                    final double d = Double.parseDouble(stoppedMatcher.group(1));
                    totalTime += d;
                    maxTime = Math.max(maxTime, d);
                }
            }
            else
            {
                final Matcher gcMatcher = GC_PATTERN.matcher(line);
                if (gcMatcher.matches())
                {
                    final Date dateTime = DATESTAMP_FORMAT.parse(gcMatcher.group(1));

                    if (dateTime.after(monitoringStartTimestamp))
                    {
                        if (null != currentDateTime)
                        {
                            if (totalTime != 0)
                            {
                                printGcPauseLine(pauseWriter, currentDateTime.getTime(), totalTime, maxTime);
                                printGcPercentLine(percentWriter, dateTime.getTime(), currentDateTime.getTime(), totalTime);
                                totalTime = 0;
                                maxTime = 0;
                            }
                        }

                        currentDateTime = dateTime;
                    }
                }
            }
        }

        if (currentDateTime != null && totalTime != 0)
        {
            printGcPauseLine(pauseWriter, currentDateTime.getTime(), totalTime, maxTime);
        }
    }

    public static void closeFile(final String pauseFilename, final PrintWriter pauseWriter)
    {
        pauseWriter.flush();
        pauseWriter.close();
        System.out.printf("Written data file: %s%n", pauseFilename);
    }

    public static void printGnuPlotCommand(final String commandFilename, final String command,
                                           final String imageFilename, final String pauseFilename,
                                           final String percentFilename) throws FileNotFoundException
    {
        final PrintWriter plotWriter = new PrintWriter(commandFilename);
        plotWriter.printf(command, imageFilename, pauseFilename, pauseFilename, percentFilename);
        plotWriter.flush();
        plotWriter.close();
        System.out.printf("Written plot file: %s%n", commandFilename);
        System.out.printf("Use the gnuplot command (image file: %s):%n", imageFilename);
        System.out.printf(">  gnuplot -e 'load \"%s\"'%n", commandFilename);
        System.out.println();
    }

    public static void printGcPauseLine(final PrintWriter w, final long timestamp, final double totalTime, final double maxTime)
    {
        w.printf("%d %d %d%n", timestamp / 1000, (long)(totalTime * 1000000), (long)(maxTime * 1000000));
    }

    public static void printGcPercentLine(final PrintWriter w, final long timestamp, final long lastTimestamp, final double totalTime)
    {
        final double timeBetweenPause = timestamp - lastTimestamp;
        final double pauseTimeMillis = (long)(totalTime * 1000);
        w.printf("%d %f%n", timestamp / 1000, pauseTimeMillis / timeBetweenPause);
    }
}
