# Generated by Buildr 1.3.4, change to your liking
# Version number for this release
VERSION_NUMBER = "1.0.0"
# Group identifier for your projects
GROUP = "gh"
COPYRIGHT = ""

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://www.ibiblio.org/maven2/"

desc "The Gh project"
define "gh" do

  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT

  system("git pull github master")

  compile.with("pircbot-1.4.6.jar", "mysql-connector-java-5.1.6-bin.jar")
#    compile.with("mysql:mysql-connector-java:jar:5.1.6", "pircbot:pircbot:jar:1.4.2")
  package(:jar).with :manifest=>_('src/main/MANIFEST.MF')

end