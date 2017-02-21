# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the setFlags in this file are appended to setFlags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:

-keepattributes SourceFile,LineNumberTable,EnclosingMethod

-dontwarn me.piebridge.brevent.server.HideApiOverride*

-keep class me.piebridge.brevent.loader.Brevent { public *; }

-keep class me.piebridge.EventHandler { *; }

-keep class me.piebridge.LogReader { *; }
