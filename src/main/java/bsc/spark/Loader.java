package bsc.spark;

//import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;


public class Loader {

        public static void main(String[] args) throws Exception {
            String usageHelp = "Usage: Loader <localDirPath> <HDFSDirPath> [<directory where core-site.xml and hdfs-site.xml resides>]";
            usageHelp += "\n Note: typical path for hdfs config files is /etc/hadoop/conf";


            if (args.length < 2) {
                    System.out.println(usageHelp);
            } else {
                    Path source = new Path(args[0]);
                    Path target = new Path(args[1]);                        
                    String hdfsConfigurationDirectory = null;
                    if (args.length > 2) hdfsConfigurationDirectory = args[2];
                    copyFiles(source, target, hdfsConfigurationDirectory);
            }
        }

        static void copyFiles(Path source, Path target, String hdfsConfigurationDirectory) throws Exception {
            //bin/hadoop dfs -copyFromLocal test.txt /tmp/test2.txt
            System.out.println("bsc.spark_mn3.Loader: Configuring HDFS FileSystem...");
            
            Configuration conf = new Configuration();
            if (hdfsConfigurationDirectory != null) {
                if (!(new File(hdfsConfigurationDirectory+"/core-site.xml")).exists()) 
                    throw new Exception("Configuration file "+hdfsConfigurationDirectory+"/core-site.xml not found");
                if (!(new File(hdfsConfigurationDirectory+"/hdfs-site.xml")).exists()) 
                    throw new Exception("bsc.spark_mn3.Loader: Configuration file "+hdfsConfigurationDirectory+"/hdfs-site.xml not found");
                System.out.println("bsc.spark_mn3.Loader: Configuring FileSystem with "+hdfsConfigurationDirectory+"/core-site.xml");
                conf.addResource(new Path(hdfsConfigurationDirectory+"/core-site.xml"));
                System.out.println("bsc.spark_mn3.Loader: Configuring FileSystem with "+hdfsConfigurationDirectory+"/hdfs-site.xml");
                conf.addResource(new Path(hdfsConfigurationDirectory+"/hdfs-site.xml"));
            } else {
                System.out.println("bsc.spark_mn3.Loader: Configuring default FileSystem (no <hdfsConfigurationDirectory> specified");
            }
            FileSystem fs = FileSystem.get(conf);
            System.out.println("bsc.spark_mn3.Loader: fs.getUri() = "+fs.getUri());
            System.out.println("bsc.spark_mn3.Loader: fs.getHomeDirectory() = "+fs.getHomeDirectory());
            System.out.println("bsc.spark_mn3.Loader: Copying directory "+source+" to "+target);
            fs.copyFromLocalFile(source, target);
            System.out.println("bsc.spark_mn3.Loader: copied.");
            System.out.println("bsc.spark_mn3.Loader: Note: This command should be equivalent to command line /bin/hadoop dfs -copyFromLocal "+source+" "+target);            
        }

}
