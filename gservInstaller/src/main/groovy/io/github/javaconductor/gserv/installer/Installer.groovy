package io.github.javaconductor.gserv.installer

import org.apache.commons.io.IOUtils

import java.nio.file.Files

class Installer {
    static def VERSION_FILENAME = "version.txt";
    public static final String GSERV_HOME = ".gserv"

    static void main(String[] args){
        Installer installer = new Installer();

        /// get the user's home directory
        File userHome = EnvPathUtils.homeDir()
        File gservHome = new File( userHome, GSERV_HOME);
        if (gservHome.exists()){
            /// uninstall previous version - look for Version.txt for the version number
            File versionFile=new File(gservHome, VERSION_FILENAME)
            if (!versionFile.exists()){
                // clear the directory
                Files.delete(gservHome.toPath());
            }else{
                installer.unInstall(gservHome)
            }
        }//if
        installer.install(gservHome);
    }

    EnvPathUtils envPathUtils
    def Installer(){
        /// PRE- CONDITION: io.github.javaconductor.gserv.installer.Installer MUST have 3 things on its classpath:
        //// gserv.jar
        //// gserv.sh
        //// version.txt

        // validate presence of files
        def bOk = getJarStream() && getScriptStream() && getVersionStream()
        if (!bOk){
            System.err.println("Bad installer jar.  Nothing to install!!")
            throw new InstallationException("Bad installer jar.  Nothing to install!!");
        }

    }

    def copyFile( File destDir, File sourceFile){
        if ( !destDir.exists()){
            Files.createDirectories(destDir.toPath())
        }
        File outFile = new File ( destDir, sourceFile.name);
        OutputStream os = new FileOutputStream(outFile);
        Files.copy(sourceFile.toPath(), os )
        os.close();
        outFile
    }

    def install(File gservHome){
        envPathUtils = new EnvPathUtils(gservHome)
        // destination dirs
        File dirBin = new File(gservHome, "bin")
        File dirScripts = new File(gservHome, "scripts")

        /// 1. copy gserv jar to ~/.gserv/bin - it should be embedded in the io.github.javaconductor.gserv.installer.Installer.jar (classpath resource)
        // The gserv.jar file should be on the classpath
        InputStream inJar= getJarStream()
        createFile( dirBin, "gserv.jar", inJar)

        /// 2. Create gserv script in ~/.gserv/gserv
        ///  chmod the script to X for all
        InputStream inScript= getScriptStream();
        File f = createFile( dirScripts, "gserv", inScript)
        f.setExecutable(true, false)

        /// 2b. Add a file version.txt with the Version/license info for gServ
        InputStream inVersion = getVersionStream();
        createFile( gservHome, VERSION_FILENAME, inVersion)

        ///3. Add gserv to the PATH
        /// /// add the ~/.gserv/scripts to the PATH in its own line (append)
        envPathUtils.addScriptDirToPath(dirScripts)

        ///5. report what version was installed and where
        /// What Version - get it from the version.txt in the installer jar
        File vFile = new File(gservHome,VERSION_FILENAME)
        def versionProps = new Properties()
        versionProps.load(new FileReader(vFile))
        def version = versionProps.version
        println "gServ v$version installed [${gservHome.absolutePath}]."

        ///6. invite user to run the new gserv command
        println "To test installation: type 'gserv' at prompt."
    }



    InputStream resourceAsStream(name){
        Installer.class.getClassLoader().getResourceAsStream(name);
    }

    InputStream getJarStream() {
        resourceAsStream("gserv.jar")
    }

    InputStream getVersionStream() {
        resourceAsStream("version.txt")
    }

    InputStream getScriptStream() {
        resourceAsStream("gserv.sh")
    }


    File createFile( destDir, filename, InputStream inScript){
        def outFile = new File(destDir, filename)
        if ( !destDir.exists()){
            Files.createDirectories(destDir.toPath())
        }
        FileOutputStream fileOutputStream = new FileOutputStream(
                outFile
        );
        IOUtils.copy(inScript, fileOutputStream)
        fileOutputStream.close()
        outFile
    }

    def unInstall(File gservHome){
        File versionFile=new File(gservHome, VERSION_FILENAME)
        if (!versionFile.exists()){
            // clear the directory
            Files.delete(gservHome.toPath());
        }else{
            Properties p = new Properties();
            def fis = new FileInputStream(versionFile)
            p.load( fis )
            fis.close();
            if( p.name != "gServ" ){
                Files.delete(gservHome.toPath());
            }else {
                new UnInstaller().unInstall(gservHome, p.version);
            }
        }
    }//
}

