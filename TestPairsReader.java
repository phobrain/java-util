package com.priot.testpfr;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.*;
//import java.util.concurrent.*;

//import java.util.Arrays;
//import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import com.priot.util.Stdio;
import com.priot.util.PairFilesReader;
import com.priot.util.PairFileAggregator;

public class TestPairsReader extends Stdio {

    private static void usage(String msg) {
        System.err.println(
            "Usage: [prog] gen <n_ids> <n_files>\n" +
            "       [prog] <dir>");
        if (msg != null) {
            System.err.println("\t" + msg);
        }
        err("Fix magic, then quickly defork and defrock.");
    }

    //private static String pairs_dump_fname = null;
    //private static PrintStream pairOut = null;

    private static long start_time = System.currentTimeMillis();
    private static long write_time = 0;

    private static List<File> dirs = new ArrayList<>();
    private static List<String> ids = null;

    private static void addPath(String path) {

        File f = new File(path);

        if (!f.exists()) {
            usage("Path does not exist: " + path);
        }
        if (!f.isDirectory()) {
            usage("Path not a directory: " + path);
        }

        dirs.add(f);
    }

    static PairFileAggregator aggregator = null;

    static void makeAggregator() {
        if (dirs.size() == 0) {
            err("makeAggregator: no dirs");
        }
        try {
            //if ("avg".equals(type)) {
            pout("avg dirs: " + dirs.toString());
            aggregator = new PairFileAggregator("testAvg", dirs, true, 0.0);
            //} else if ("neg".equals(type)) {
            //     // 1+ neg dirs to screen files by cutoff and count 
            //     aggregator = new PairFileAggregator(name, dirs, false, 
            //                                              0.62); // MAGIC
            //}
        } catch (Exception e) {
            err("Aggregator launch splat! " + e);
        }
    }

    public static void main(String[] args) {
  
        if (args.length < 1) { // list_file <dirs..>
            usage("no args");
        }

        int lineNum = 0;
        long t0 = System.currentTimeMillis();

        if("gen".equals(args[0])) {
            if (args.length != 3) {
                usage("gen");
            }
            try {
                int nids   = Integer.parseInt(args[1]);
                int nfiles = Integer.parseInt(args[2]);

                int npairs = (nids * (nids-1)) / 2;
                int MOD = npairs / 100;

                String dirname = "input_" + nids + "_ids_" + nfiles + "_files";
                File dir = new File(dirname);
                if (dir.exists()) {
                    pout("Exists: " + dirname);
                    pout("Sleeping 5 sec and recreating");
                    Thread.sleep(5000);
                    String[] fnames = dir.list();
                    for (String fname : fnames) {
                        File f = new File(dirname + "/" + fname);
                        if (!f.delete()) {
                            err("Delete failed: " + f.getPath());
                        }
                    }
                    if (!dir.delete()) {
                        err("Delete failed: " + dir.getPath());
                    };
                }
                if (!dir.mkdir()) {
                    err("mkdir failed: " + dirname);
                }

                dirname += "/";
                PrintStream[] outs = new PrintStream[nfiles];
                for (int i=0; i<nfiles; i++) {
                    outs[i] = new PrintStream(dirname + i + ".pairs");
                }

                pout("Writing single-thread: " + npairs + 
				" lines to " + nfiles + " files" +
				" in " + dirname +
				" in 5 sec");

		try { Thread.sleep(5000); } catch (Exception e) {} 

                pout("Writing!");
	        pout("Reports/predictions will be updated inline each 1%");
		pout("The first 1% is the hardest to wait for");

                ids = new ArrayList<>();

                double dpair = 0.0;

                for (int i=0; i<nids-1; i++) {

                    String id1 = "" + i + ":" + i;

                    ids.add(id1);

                    for (int j=i+1; j<nids; j++) {

                        dpair += 1;

                        String line = id1 + " " + j + ":" + j + " " +
                                        dpair + " " + (dpair + 1.0) + "\n";
                        
                        dpair += 1;

                        for (PrintStream out : outs) {
                            out.print(line);
                        }
                        lineNum++;

                        if (lineNum % MOD == 0) {
                            long t1 = System.currentTimeMillis();
                            long dt = t1 - t0;
                            double rate = (double)lineNum / dt;
                            int remainder = npairs - lineNum;
                            long finish = t1 + (long)(remainder / rate);

                            String s = "\rdone: " + ((int)((100.0*lineNum) 
                                                           / npairs)) +
                                    "%%  expect: " + new Date(finish) +
                                    "                       ";
                            System.err.printf(s);
                        }
                    }
                }
                for (PrintStream out : outs) {
                    out.close();
                }

                ids.add("" + (nids-1) + ":" + (nids-1));

                if (ids.size() != nids) {
                    err("Oops: list size " + ids.size() + " != " + nids);
                }

                PrintStream listfile = new PrintStream(dirname + "idlist");
                for (String id : ids) {
                    listfile.println(id);
                }
                listfile.close();
                pout("\nPrinted " + nfiles + " files w/ " + nids + " ids");
                pout("\tin " + dirname + " id list: " + dirname + "idlist");

            } catch (Exception e) {
                e.printStackTrace();
            }
            return;   
        }

        if (args.length != 1) {
            err("expected dir");
        }
        File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            err("Not a dir: " + args[0]);
        }
        try {
            ids = Files.readAllLines(Paths.get(args[0] + "/idlist"));
        } catch (Exception e) {
            e.printStackTrace();
            err("Reading ids from " + args[0] + ": " + e);
        }
        pout("Ids from " + args[0] + "/idlist: " + ids.size());
        if (ids.size() == 0) {
            err("No ids/lines in " + args[0] + "/idlist");
        }

        addPath(args[0]);

        makeAggregator();

        PairFilesReader reader = PairFilesReader.getReader();
        reader.init();

        try {

            int npairs = (ids.size() * (ids.size()-1)) / 2;
            int MOD = npairs / 100;

            pout("Ids: " + ids.size() + " Pairs: " + npairs);

            double checkDouble = 0.0;

            for (int i=0; i<ids.size()-1; i++) {

                String id1 = ids.get(i);

                for (int j=i+1; j<ids.size(); j++) {

                    String id2 = ids.get(j);
                    //pout("> " + id1 + "  " + id2);

                    // advance nn pairs files
                    lineNum++;
                    if (!reader.next(lineNum, id1, id2)) {
                        err("reader is done too soon, lineNum " + lineNum);
                    }
                    double[] vals = aggregator.readLine();
                    checkDouble += 1.0;
                    if (vals[0] != checkDouble) {
                        err("Line " + lineNum +
                                ": Got vals[0] " + vals[0] + 
                                " expected " + checkDouble);
                    }
                    checkDouble += 1.0;
                    if (vals[1] != checkDouble) {
                        err("Line " + lineNum +
                                ": Got vals[1] " + vals[1] + 
                                " expected " + checkDouble);
                    }

                    if (lineNum % MOD == 0) {
                        long t1 = System.currentTimeMillis();
                        long dt = t1 - t0;
                        double rate = (double)lineNum / dt;
                        int remainder = npairs - lineNum;
                        long finish = t1 + (long)(remainder / rate);

                        String s = "\rdone: " + ((int)((100.0*lineNum) 
                                                           / npairs)) +
                                    "%%  expect: " + new Date(finish) +
                                    "                       ";
                        System.err.printf(s);
                    }


                    //handle(bp1, bp2, pairOut);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            err("Reading: " + e);
        }

        reader.done();
        pout("DONE/PASSED");
    }


}
