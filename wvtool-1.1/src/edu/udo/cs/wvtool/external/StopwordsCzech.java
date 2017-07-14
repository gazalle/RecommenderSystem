package edu.udo.cs.wvtool.external;

import java.util.HashSet;

import edu.udo.cs.wvtool.generic.wordfilter.AbstractStopWordFilter;

/**
 * Stopwords for the German language
 * 
 * @version $Id$
 *
 */
public class StopwordsCzech extends AbstractStopWordFilter {

    /** The hashtable containing the list of stopwords */
    private static HashSet m_Stopwords = null;

    private static String[] stopWords = new String[] {
        "dnes","cz","timto","budes","budem","byli","jses","muj","svym","ta",
        "tomto","tohle","tuto","tyto","jej","zda","proc","mate","tato","kam",
        "tohoto","kdo","kteri","mi","nam","tom","tomuto","mit","nic","proto",
        "kterou","byla","toho","protoze","asi","ho","nasi","napiste","re","coz",
        "tim","takze","svych","jeji","svymi","jste","aj","tu","tedy","teto  ",
        "bylo","kde","ke","prave","ji","nad","nejsou","ci","pod","tema",
        "mezi","pres","ty","pak","vam","ani","kdyz","vsak","ne","jsem",
        "tento","clanku","clanky","aby","jsme","pred","pta","jejich","byl","jeste",
        "az","bez","take","pouze","prvni","vase","ktera","nas","novy","tipy",
        "pokud","muze","design","strana","jeho","sve","jine","zpravy","nove","neni",
        "vas","jen","podle","zde","clanek","uz","email","byt","vice","bude",
        "jiz","nez","ktery","by","ktere","co","nebo","ten","tak","ma",
        "pri","od","po","jsou","jak","dalsi","ale","si","ve","to",
        "jako","za","zpet","ze","do","pro","je","na"};        
        
        static {
            if (m_Stopwords == null) {
                m_Stopwords = new HashSet();
                
                for (int i = 0; i < stopWords.length; i++) {
                    m_Stopwords.add(stopWords[i]);
                }
            }

        }

        public boolean isStopword(String str) {

            return m_Stopwords.contains(str.toLowerCase());
        }

}
