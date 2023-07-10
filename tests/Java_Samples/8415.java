package special;

import misc.*;
import java.util.*;
import java.io.*;
import misc.intervaltree.*;
import java.util.Map.*;

public class IntronRetentionCGFF1 {

    private static String gffFilename = null;

    private static Map mappingFilenameMethodMap = new LinkedHashMap();

    private static Map mappingMethodMap = null;

    private static String outFilename = null;

    private static int joinFactor = 2;

    private static float identityCutoff = 0.95F;

    private static int minimumOverlap = 8;

    private static void paraProc(String[] args) {
        int i;
        for (i = 0; i < args.length; i++) {
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-M")) {
                mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
                i += 2;
            } else if (args[i].equals("-O")) {
                outFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-J")) {
                joinFactor = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-ID")) {
                identityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            } else if (args[i].equals("-min")) {
                minimumOverlap = Integer.parseInt(args[i + 1]);
                i++;
            }
        }
        if (gffFilename == null) {
            System.err.println("canonical GFF filename (-GFF) not assigned");
            System.exit(1);
        }
        mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator", System.getProperty("java.class.path"), "misc");
        if (mappingFilenameMethodMap.size() <= 0) {
            System.err.println("mapping method/filename (-M) not assigned, available methods:");
            for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext(); ) {
                System.out.println(iterator.next());
            }
            System.exit(1);
        }
        for (Iterator methodIterator = mappingFilenameMethodMap.values().iterator(); methodIterator.hasNext(); ) {
            String mappingMethod = (String) methodIterator.next();
            if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
                System.err.println("assigned mapping method (-M) not exists: " + mappingMethod + ", available methods:");
                for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext(); ) {
                    System.out.println(iterator.next());
                }
                System.exit(1);
            }
        }
        if (outFilename == null) {
            System.err.println("output filename (-O) not assigned");
            System.exit(1);
        }
        if (minimumOverlap < 1) {
            System.err.println("minimum overlap (-min) less than 1");
            System.exit(1);
        }
        System.out.println("program: IntronRetentionCGFF1");
        System.out.println("canonical GFF filename (-GFF): " + gffFilename);
        System.out.println("mapping method/filename (-M):");
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output filename (-O): " + outFilename);
        System.out.println("block join factor (-J): " + joinFactor);
        System.out.println("identity cutoff (-ID): " + identityCutoff);
        System.out.println("minimum overlap (-min): " + minimumOverlap);
        System.out.println();
    }

    public static void main(String args[]) {
        paraProc(args);
        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        CanonicalGFF intronCGFF = Util.getIntronicCGFF(cgff);
        Map geneIntronCntMap = new TreeMap();
        Map geneIntronIcnsMap = new TreeMap();
        for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator(); mriIterator.hasNext(); ) {
            Map.Entry entry = (Entry) mriIterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            int mappedReadCnt = 0;
            int processedLines = 0;
            for (MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod); mappingResultIterator.hasNext(); ) {
                mappedReadCnt++;
                ArrayList mappingRecords = (ArrayList) mappingResultIterator.next();
                processedLines += mappingRecords.size();
                if (mappingResultIterator.getBestIdentity() < identityCutoff) continue;
                ArrayList acceptedRecords = new ArrayList();
                for (int i = 0; i < mappingRecords.size(); i++) {
                    AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
                    if (record.identity >= mappingResultIterator.getBestIdentity()) {
                        if (joinFactor > 0) record.nearbyJoin(joinFactor);
                        acceptedRecords.add(record);
                    }
                }
                if (acceptedRecords.size() > 1) continue;
                AlignmentRecord record = (AlignmentRecord) acceptedRecords.get(0);
                if (record.numBlocks > 1) continue;
                Set hitGenes = cgff.getRelatedGenes(record.chr, record.getMappingIntervals(), false, false, minimumOverlap, true);
                if (hitGenes.size() > 1) continue;
                hitGenes = intronCGFF.getRelatedGenes(record.chr, record.getMappingIntervals(), true, false, minimumOverlap, true);
                if (hitGenes.size() != 1) continue;
                Interval alignmentInterval = (Interval) record.getMappingIntervals().iterator().next();
                GenomeInterval geneRegion = (GenomeInterval) hitGenes.iterator().next();
                String geneID = (String) geneRegion.getUserObject();
                Set intronRegions = (Set) intronCGFF.geneExonRegionMap.get(geneID);
                int intronNo = 0;
                Iterator intronIterator = intronRegions.iterator();
                for (; intronIterator.hasNext(); ) {
                    Interval intron = (Interval) intronIterator.next();
                    intronNo++;
                    if (intron.intersect(alignmentInterval, minimumOverlap) == false) continue;
                    Map intronCntMap;
                    if (geneIntronCntMap.containsKey(geneID)) {
                        intronCntMap = (Map) geneIntronCntMap.get(geneID);
                    } else {
                        intronCntMap = new TreeMap();
                        geneIntronCntMap.put(geneID, intronCntMap);
                    }
                    if (intronCntMap.containsKey(intronNo)) {
                        int val = ((Integer) intronCntMap.get(intronNo)).intValue();
                        intronCntMap.put(intronNo, val + 1);
                    } else {
                        intronCntMap.put(intronNo, 1);
                    }
                    Map intronIcnsMap;
                    if (geneIntronIcnsMap.containsKey(geneID)) {
                        intronIcnsMap = (Map) geneIntronIcnsMap.get(geneID);
                    } else {
                        intronIcnsMap = new TreeMap();
                        geneIntronIcnsMap.put(geneID, intronIcnsMap);
                    }
                    Set icns;
                    if (intronIcnsMap.containsKey(intronNo)) {
                        icns = (Set) intronIcnsMap.get(intronNo);
                    } else {
                        icns = new TreeSet();
                        intronIcnsMap.put(intronNo, icns);
                    }
                    IntervalCoverageNode thisIcn = new IntervalCoverageNode(alignmentInterval.getStart(), alignmentInterval.getStop(), mappingResultIterator.getReadID());
                    Set overlapIcns = new HashSet();
                    for (Iterator icnIterator = icns.iterator(); icnIterator.hasNext(); ) {
                        IntervalCoverageNode otherIcn = (IntervalCoverageNode) icnIterator.next();
                        if (thisIcn.intersect(otherIcn)) overlapIcns.add(otherIcn);
                    }
                    if (overlapIcns.size() == 0) {
                        icns.add(thisIcn);
                    } else {
                        overlapIcns.add(thisIcn);
                        IntervalCoverageNode newIcn = thisIcn.combine(overlapIcns);
                        icns.removeAll(overlapIcns);
                        icns.add(newIcn);
                    }
                }
            }
            System.out.println(mappedReadCnt + " mapped reads (" + processedLines + " lines) in " + mappingFilename);
        }
        try {
            FileWriter fw = new FileWriter(outFilename);
            fw.write("#geneID" + "\t" + "intronNo" + "\t" + "iStart" + "\t" + "iStop" + "\t" + "read" + "\t" + "rStart" + "\t" + "rStop" + "\t" + "array" + "\n");
            for (Iterator geneIterator = geneIntronIcnsMap.keySet().iterator(); geneIterator.hasNext(); ) {
                Object geneID = geneIterator.next();
                Set intronRegions = (Set) intronCGFF.geneExonRegionMap.get(geneID);
                Interval[] intronRegionArray = (Interval[]) intronRegions.toArray(new Interval[intronRegions.size()]);
                Map intronIcnsMap = (Map) geneIntronIcnsMap.get(geneID);
                Map intronCntMap = (Map) geneIntronCntMap.get(geneID);
                for (Iterator intronIterator = intronIcnsMap.keySet().iterator(); intronIterator.hasNext(); ) {
                    int intronNo = ((Integer) intronIterator.next()).intValue();
                    Set icnSet = (Set) intronIcnsMap.get(intronNo);
                    Interval intronInterval = intronRegionArray[intronNo - 1];
                    for (Iterator icnIterator = icnSet.iterator(); icnIterator.hasNext(); ) {
                        IntervalCoverageNode icn = (IntervalCoverageNode) icnIterator.next();
                        fw.write(geneID + "\t" + intronNo + "\t" + intronInterval.getStart() + "\t" + intronInterval.getStop() + "\t" + intronCntMap.get(intronNo) + "\t" + icn.getStart() + "\t" + icn.getStop() + "\t" + Arrays.toString(icn.getCoverageArray()) + "\n");
                    }
                }
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
