package com.priot.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

import javax.naming.InvalidNameException;

public class MiscUtil {

    public static class ID implements Comparable {

        final public static String SPLIT_STR = "/";

        /** canonical for xx12,xx21 table ordering */

        public static void sortIds(String[] pairIds, String id0, String id1) 
                throws InvalidNameException {

            ID bid0 = new ID(id0);
            ID bid1 = new ID(id1);

            if (ID.compare(bid0, bid1) < 0) {
                pairIds[0] = id0;
                pairIds[1] = id1;
            } else {
                pairIds[0] = id1;
                pairIds[1] = id0;
            }
        }

        public static String[] sortIds(String id0, String id1)
                throws InvalidNameException {

            String[] pairIds = new String[2];
            sortIds(pairIds, id0, id1);
            return pairIds;
        }

		@Override
		public int compareTo(Object o) {
			ID idx = (ID) o;
            return compare(this, idx);
		}

        public static int compare(ID id, ID idx) {
			if (id.arch < idx.arch) return -1;
			if (id.arch > idx.arch) return 1;
			if (id.seq < idx.seq) return -1;
			if (id.seq > idx.seq) return 1;
            // seq ==; seq2 follows python sort?
            int ct = 0;
            if (id.seq2 == -1) ct++;
            if (idx.seq2 == -1) ct++;
            if (ct == 1) {
                if (id.seq2 == -1) {
                    return 1; // reversed
                }
                return -1;
            }
			if (id.seq2 < idx.seq2) return -1;
			if (id.seq2 > idx.seq2) return 1;
            // hacky
            if (id.id.endsWith(Integer.toString(id.seq))) {  // HACK
                //System.out.println("HHH " + id.id + " " + id.seq);
                return 1;
            }
            if (idx.id.endsWith(Integer.toString(idx.seq))) {  // HACK
                //System.out.println("vvv " + idx.id + " " + idx.seq);
                return -1;
            }
            return id.id.compareTo(idx.id);
        }

        public String fnameBody;

		public int arch = -1;
		public int seq = -1;
        public int seq2 = -1;
        public String tag = null;

        public boolean hardMatch = false;

        public String id;

        public ID(String id) throws InvalidNameException {

            String[] ii = id.split(SPLIT_STR);
            if (ii.length != 2) {
                throw new InvalidNameException("Bad split on '" + SPLIT_STR +
                                                "' " + id);
            }
            try {
                this.arch = Integer.parseInt(ii[0]);
            } catch (NumberFormatException nfe) {
                throw new InvalidNameException("Parsing arch on '" + SPLIT_STR +
                                                "' " + id +
                                                " " + nfe);
            }
            if (arch < 1) {
                throw new InvalidNameException("Invalid arch: " + arch);
            }
            // somewhat trusting
            this.fnameBody = "unk";
            this.id = id;
            parseSseq(ii[1]);
        }

        public ID(int arch, String fnameBody) throws InvalidNameException {

            if (arch < 1) {
                throw new InvalidNameException("Invalid arch: " + arch);
            }
            this.arch = arch;
            int ix = fnameBody.indexOf(".");
            if (ix != -1) {
                fnameBody = fnameBody.substring(0, ix);
            }
            this.fnameBody = fnameBody;

            // fnameBody -> id

            int start = 0;
            if (fnameBody.startsWith("img")  ||  
                fnameBody.startsWith("IMG")  ||  
                fnameBody.startsWith("_MG")  || 
                fnameBody.startsWith("DSC")) {
                start = 3;
                if (fnameBody.charAt(start) == '_') {
                    start++;
                }
            }
            while (fnameBody.charAt(start) == '0') {
                start++;
            }
            int end = fnameBody.length();
            if (fnameBody.endsWith("_sm")) {
                end -= 3;
            }
            String sseq = fnameBody.substring(start, end);
            id = "" + arch + SPLIT_STR + sseq;

            parseSseq(sseq);
        }

        private void parseSseq(String sseq) throws InvalidNameException {

            // parse sseq into: seq [seq2]

            int start = -1;
            for (int i=0; i<sseq.length(); i++) {
                char c = sseq.charAt(i);
                if (c == '0') {
                    continue;
                }
                if (Character.isDigit(sseq.charAt(i))) {
                    start = i;
                    break;
                }
            }
            if (start == -1) {
                throw new RuntimeException(
                        "ID: Expected a number in: " + sseq);
            }
            int end = -1;
            for (int i=start+1; i<sseq.length(); i++) {
                if (!Character.isDigit(sseq.charAt(i))) {
                    end = i;
                    break;
                }
            }
            if (end == -1) {
                end = sseq.length();
            }
            String ss = sseq.substring(start, end);
            //System.out.println("id sseq " + id + " " + sseq);
            try {
                seq = Integer.parseInt(ss);
            } catch (NumberFormatException nfe) {
                throw new InvalidNameException(
                        "ID: Expected a sequence number: " + 
                        sseq + ": " + nfe);
            }
            
            if (end < sseq.length()) {

                // try for seq2: 
                //  1234-[1-5] for files
                //  1234=[1-5] for keywords

                char c = sseq.charAt(end);
                if (c == '=') {
                    hardMatch = true;
                    c = '-';
                }
                //System.out.println("C " + c);
                if (c == '-') {
                    start = end + 1;
                    end = -1;
                    for (int i=start+1; i<sseq.length(); i++) {
                        if (!Character.isDigit(sseq.charAt(i))) {
                            end = i;
                            break;
                        }
                    }
                    if (end == -1) end = sseq.length();

                    if (start == end) {
                        throw new InvalidNameException(
                                        "ID: id part2 hanging dash: " + 
                                        sseq);
                    }
                    String sseq2 = sseq.substring(start, end);
                    try {
                        seq2 = Integer.parseInt(sseq2);
                    } catch (NumberFormatException nfe) {
                        throw new InvalidNameException("ID: parsing seq2: " + 
                                     sseq + ": " + nfe);
                    }
                }
                if (end < sseq.length()) {
                    tag = sseq.substring(end);
                }
            }
        }

    }

    public static class FileRec implements Comparable {
	    public int ct;

		public String fname;

        public ID id;

		public File file;
		public boolean vertical;
		public double[] histogram;

		@Override
		public int compareTo(Object o) {
			FileRec fr = (FileRec) o;
            int cmp = id.compareTo(fr.id);
            if (cmp != 0) return cmp;
			return fname.compareTo(fr.fname);
		}

        public FileRec(String path, boolean expectArchSeq) 
                throws InvalidNameException {

            file = new File(path);
            if (!file.isFile()) {
                throw new RuntimeException("Not a file: " + path);
            }

            // get fname, check reqd substrings

            int fi = path.lastIndexOf("/");
            if (fi == -1) {
                throw new RuntimeException("No '/': " + path);
            }
            fname = path.substring(fi+1);
            if (fname.length() == 0) {
                throw new RuntimeException("No fname: " + path);
            }
            String s = fname.toLowerCase();
            int endFnameBody = s.indexOf(".jpg");
            if (endFnameBody == -1) {
                endFnameBody = s.indexOf(".jpeg");
            }
            if (endFnameBody == -1) {
                throw new RuntimeException(
                           "FileRec: Expected '.jpg' or '.jpeg': " + path);
            }
            if (!s.contains("img")  &&
                !s.contains("_mg")  &&
                !s.contains("dsc")) {
                throw new RuntimeException(
                           "FileRec: Expected 'img', '_mg', 'dsc': " + path);
            }

            if (expectArchSeq) {

                String fnameBody = fname.substring(0, endFnameBody);

                // get archive

                int arch = -1;

                for (int i=fi-1; i>-1; i--) {
                    if (path.charAt(i) == '/') {
                        String a = path.substring(i+1, fi);
                        try {
                            arch = Integer.parseInt(a);
                        } catch (NumberFormatException nfe) {
                            throw new RuntimeException("Parsing archive: [" + 
                                               a + "] [" + path + "]: " + nfe);
                        }
                        break;
                    }
                }
                if (arch == -1) {
                    throw new RuntimeException("No archive: " + path);
                }
                id = new ID(arch, fnameBody);
            }
        }
    }

    public static int parseSeq(String s) throws InvalidNameException {
        if (!s.toLowerCase().contains("img")  &&
            !s.toLowerCase().contains("_mg")  &&
            !s.toLowerCase().contains("dsc")) {
            throw new InvalidNameException(
                    "parseSeq: Expected 'img', '_mg', 'dsc': " + s);
        }
        int start = -1;
        int end = -1;
        for (int i=0; i<s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                start = i;
                break;
            }
        }
        if (start == -1) {
            throw new InvalidNameException("parseSeq: Expected a number: " + s);
        }
        for (int i=start+1; i<s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                end = i;
                break;
            }
        }
        if (end == -1) {
            end = s.length();
        } else {
/*
ignoring secondary numbers
            for (int i=end+2; i<s.length(); i++) {
                if (Character.isDigit(s.charAt(i))) {
                    throw new Exception("More than 1 number: " + s);
                }
            }
*/
        }

        s = s.substring(start, end);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            throw new InvalidNameException("Expected int: " + s + ": " + nfe);
        }
    }

    public static List<Integer> parseInts(String val, int limit) {
        val = val.trim();
        String ss[] = val.split(" ");
        List<Integer> l = new ArrayList<>();
        int count = 0;
        for (String s : ss) {
            if (s.trim().length() != 0) {
                l.add(Integer.parseInt(s));
                count++;
                if (count > limit) break;
            }
        }
        return l;
    }

    public static int normalizeFracDim(String s) {
        try {
            float f = Float.parseFloat(s);
            f *= 1000.0d;
            return Math.round(f);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Bad fracDim: " + s);
        }
    }

    public static String formatInterval(final long l) {
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - 
				TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - 
				TimeUnit.HOURS.toMillis(hr) - 
				TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(l - 
				TimeUnit.HOURS.toMillis(hr) - 
				TimeUnit.MINUTES.toMillis(min) - 
				TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
    }

    public static List<Integer> intList(String s) {

        List<Integer> ret = new ArrayList<>();

        if (s == null) {
            return ret;
        }

        String ss[] = s.split(" ");

        try {
            for (String si : ss) {
                ret.add(Integer.parseInt(si));
            }
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Parsing " + s, nfe);
        }
        return ret;
    }

    final static private char digits[] =
      // 0       8       16      24      32      40      48      56     63
      // v       v       v       v       v       v       v       v      v
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_-".toCharArray();

    final static private Map<Character, Integer> digitMap = new HashMap<>();
    static {
        for (int i=0; i<digits.length; i++) {
            digitMap.put(digits[i], i);
        }
    }

    // sacrifice 16510910
    public final static String NULL_BASE64 = "____";

    public static int base64ToInt(String s) {
        if (s == null) {
            throw new RuntimeException("NULL str");
        }
        int res = 0;
        for (int i=0; i<s.length(); i++) {
            Character c = s.charAt(i);
            if (c == null) {
                throw new RuntimeException("UNK char " + 
                             Character.isLetterOrDigit(c) + " " +
                             Character.isWhitespace(c) + " " +
                             Character.isSpaceChar(c) + " " +
                             Character.getNumericValue(c));
            }
            res = (res << 6) + digitMap.get(s.charAt(i));
        }
        return res;
    }

    public static int[] base64ToIntArray(String s) {
        if (s == null) {
            throw new RuntimeException("NULL str");
        }
        String tt[] = s.split(",");
        int ret[] = new int[tt.length];
        for (int i=0; i< tt.length; i++) {
            ret[i] = base64ToInt(tt[i]);
        }
        return ret;
    }

    public static void copyFileToFile(final File src, final File dest) 
            throws IOException {
        copyInputStreamToFile(new FileInputStream(src), dest);
        dest.setLastModified(src.lastModified());
    }

    public static void copyInputStreamToFile(final InputStream in, 
                                             final File dest)
            throws IOException {
        copyInputStreamToOutputStream(in, new FileOutputStream(dest));
    }


    public static void copyInputStreamToOutputStream(final InputStream in,
                                                     final OutputStream out) 
            throws IOException {
        try {
            try {
                final byte[] buffer = new byte[1024];
                int n;
                while ((n = in.read(buffer)) != -1)
                    out.write(buffer, 0, n);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static Random rand = new Random();

    public static void shuffleInts(int[] arr) {
        for (int k=arr.length-1; k>-1; k--) {
            int kk = rand.nextInt(k+1);
            int a = arr[kk];
            arr[kk] = arr[k];
            arr[k] = a;
        }
    }

}
