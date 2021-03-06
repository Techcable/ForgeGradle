package net.minecraftforge.gradle.patcher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraftforge.gradle.common.Constants;

import org.gradle.api.DefaultTask;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;

class TaskSubprojectCall extends DefaultTask
{
    private Object projectDir;
    private Object callLine;
    private final List<URL> initResources = Lists.newArrayList();
    private final Map<String, Object> replacements = Maps.newHashMap();
    
    //@formatter:off
    public TaskSubprojectCall() { super(); }
    //@formatter:on
    
    @TaskAction
    public void doTask() throws IOException
    {
        // resolve replacements
        for (Entry<String, Object> entry : replacements.entrySet())
        {
            replacements.put(entry.getKey(), Constants.resolveString(entry.getValue()));
        }
        
        // extract extra initscripts
        List<File> initscripts = Lists.newArrayListWithCapacity(initResources.size());
        for (int i = 0; i < initResources.size(); i++)
        {
            File file = new File(getTemporaryDir(), "initscript"+i);
            String thing = Resources.toString(initResources.get(i), Constants.CHARSET);
            
            for (Entry<String, Object> entry : replacements.entrySet())
            {
                thing = thing.replace(entry.getKey(), (String)entry.getValue());
            }
            
            Files.write(thing, file, Constants.CHARSET);
            initscripts.add(file);
        }
        
        // get current Gradle instance
        Gradle gradle = getProject().getGradle();
        
        getProject().getLogger().lifecycle("------------------------ ");
        getProject().getLogger().lifecycle("--------SUB-CALL-------- ");
        getProject().getLogger().lifecycle("------------------------ ");
        
        // connect to project
        ProjectConnection connection = GradleConnector.newConnector()
                .useGradleUserHomeDir(gradle.getGradleUserHomeDir())
                .useInstallation(gradle.getGradleHomeDir())
                .forProjectDirectory(getProjectDir())
                .connect();
        
        //get args
        ArrayList<String> args = new ArrayList<String>(5);
        args.addAll(Splitter.on(' ').splitToList(getCallLine()));

        for (File f : initscripts)
        {
            args.add("-I" + f.getCanonicalPath());
        }
        
        // build
        connection.newBuild()
                .setStandardOutput(System.out)
                .setStandardInput(System.in)
                .setStandardError(System.err)
                .withArguments(args.toArray(new String[args.size()]))
                .setColorOutput(false)
                .run();
        
        getProject().getLogger().lifecycle("------------------------ ");
        getProject().getLogger().lifecycle("------END-SUB-CALL------ ");
        getProject().getLogger().lifecycle("------------------------ ");
    }

    public File getProjectDir()
    {
        return getProject().file(projectDir);
    }

    public void setProjectDir(Object projectDir)
    {
        this.projectDir = projectDir;
    }

    public String getCallLine()
    {
        return Constants.resolveString(callLine);
    }

    public void setCallLine(Object callLine)
    {
        this.callLine = callLine;
    }
    
    public void addInitScript(URL url)
    {
        initResources.add(url);
    }
    
    public void addReplacement(String key, Object val)
    {
        replacements.put(key, val);
    }
}
