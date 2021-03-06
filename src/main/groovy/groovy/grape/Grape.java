/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**************************************************************************************************

    SYNCHRONIZED VERSION OF GRAPE
    THIS IS A VERY BAD WORKAROUND TO FIX THE STUPID DESIGN OF GRAPE MECHANISM:
    EACH GROOVY SCRIPT ENGINE SHOULD HAVE IT'S OWN INSTANCE OF GRAPE ENGINE,
    A SINGLE SHARED INSTANCE DOESN'T MAKE SENSE
    For GroovyProcess and GroovyRemote there's no issue, because they always run on their own JVM.
    The issue can be for Groovy jobs that use @Grab annotations: if you run concurrently this kind
    of jobs the GrapeEngine instance is shared, and thus for some jobs it could be able to correctly
    update the classloader with dependencies URLs, but for others, it may fail, causing a
    ClassNotFoundError

 ***************************************************************************************************/
package groovy.grape;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.net.URI;

/**
 * Facade to GrapeEngine.
 */
public class Grape {

    public static final String AUTO_DOWNLOAD_SETTING = "autoDownload";
    public static final String DISABLE_CHECKSUMS_SETTING = "disableChecksums";

    private static boolean enableGrapes = Boolean.valueOf(System.getProperty("groovy.grape.enable", "true"));
    private static boolean enableAutoDownload = Boolean.valueOf(System.getProperty("groovy.grape.autoDownload", "true"));
    private static boolean disableChecksums = Boolean.valueOf(System.getProperty("groovy.grape.disableChecksums", "false"));
    protected static GrapeEngine instance;

    /**
     * This is a static access kill-switch.  All of the static shortcut
     * methods in this class will not work if this property is set to false.
     * By default it is set to true.
     */
    public static boolean getEnableGrapes() {
        return enableGrapes;
    }

    /**
     * This is a static access kill-switch.  All of the static shortcut
     * methods in this class will not work if this property is set to false.
     * By default it is set to true.
     */
    public static void setEnableGrapes(boolean enableGrapes) {
        Grape.enableGrapes = enableGrapes;
    }

    /**
     * This is a static access auto download enabler.  It will set the
     * 'autoDownload' value to the passed in arguments map if not already set.
     * If 'autoDownload' is set the value will not be adjusted.
     * <p>
     * This applies to the grab and resolve calls.
     * <p>
     * If it is set to false, only previously downloaded grapes
     * will be used.  This may cause failure in the grape call
     * if the library has not yet been downloaded
     * <p>
     * If it is set to true, then any jars not already downloaded will
     * automatically be downloaded.  Also, any versions expressed as a range
     * will be checked for new versions and downloaded (with dependencies)
     * if found.
     * <p>
     * By default it is set to true.
     */
    public static boolean getEnableAutoDownload() {
        return enableAutoDownload;
    }

    /**
     * This is a static access auto download enabler.  It will set the
     * 'autoDownload' value to the passed in arguments map if not already
     * set.  If 'autoDownload' is set the value will not be adjusted.
     * <p>
     * This applies to the grab and resolve calls.
     * <p>
     * If it is set to false, only previously downloaded grapes
     * will be used.  This may cause failure in the grape call
     * if the library has not yet been downloaded.
     * <p>
     * If it is set to true, then any jars not already downloaded will
     * automatically be downloaded.  Also, any versions expressed as a range
     * will be checked for new versions and downloaded (with dependencies)
     * if found. By default it is set to true.
     */
    public static void setEnableAutoDownload(boolean enableAutoDownload) {
        Grape.enableAutoDownload = enableAutoDownload;
    }

    /**
     * Global flag to ignore checksums.
     * By default it is set to false.
     */
    public static boolean getDisableChecksums() {
        return disableChecksums;
    }

    /**
     * Set global flag to ignore checksums.
     * By default it is set to false.
     */
    public static void setDisableChecksums(boolean disableChecksums) {
        Grape.disableChecksums = disableChecksums;
    }

    public static synchronized GrapeEngine getInstance() {
        if (instance == null) {
            try {
                // by default use GrapeIvy
                //TODO META-INF/services resolver?
                instance = (GrapeEngine) Class.forName("groovy.grape.GrapeIvy").newInstance();
            } catch (InstantiationException e) {
                //LOGME
            } catch (IllegalAccessException e) {
                //LOGME
            } catch (ClassNotFoundException e) {
                //LOGME
            }
        }
        return instance;
    }

    public static synchronized void grab(String endorsed) {
        if (enableGrapes) {
            GrapeEngine instance = getInstance();
            if (instance != null) {
                instance.grab(endorsed);
            }
        }
    }

    public static synchronized void grab(Map<String, Object> dependency) {
        if (enableGrapes) {
            GrapeEngine instance = getInstance();
            if (instance != null) {
                if (!dependency.containsKey(AUTO_DOWNLOAD_SETTING)) {
                    dependency.put(AUTO_DOWNLOAD_SETTING, enableAutoDownload);
                }
                if (!dependency.containsKey(DISABLE_CHECKSUMS_SETTING)) {
                    dependency.put(DISABLE_CHECKSUMS_SETTING, disableChecksums);
                }
                instance.grab(dependency);
            }
        }
    }

    public static synchronized void grab(Map<String, Object> args, Map... dependencies) {
        if (enableGrapes) {
            GrapeEngine instance = getInstance();
            if (instance != null) {
                if (!args.containsKey(AUTO_DOWNLOAD_SETTING)) {
                    args.put(AUTO_DOWNLOAD_SETTING, enableAutoDownload);
                }
                if (!args.containsKey(DISABLE_CHECKSUMS_SETTING)) {
                    args.put(DISABLE_CHECKSUMS_SETTING, disableChecksums);
                }
                instance.grab(args, dependencies);
            }
        }
    }

    public static synchronized Map<String, Map<String, List<String>>> enumerateGrapes() {
        Map<String, Map<String, List<String>>> grapes = null;
        if (enableGrapes) {
            GrapeEngine instance = getInstance();
            if (instance != null) {
                grapes = instance.enumerateGrapes();
            }
        }
        if (grapes == null) {
            return Collections.emptyMap();
        } else {
            return grapes;
        }
    }

    public static synchronized URI[] resolve(Map<String, Object> args, Map... dependencies) {
        return resolve(args, null, dependencies);
    }

    public static synchronized URI[] resolve(Map<String, Object> args, List depsInfo, Map... dependencies) {
        URI[] uris = null;
        if (enableGrapes) {
            GrapeEngine instance = getInstance();
            if (instance != null) {
                if (!args.containsKey(AUTO_DOWNLOAD_SETTING)) {
                    args.put(AUTO_DOWNLOAD_SETTING, enableAutoDownload);
                }
                if (!args.containsKey(DISABLE_CHECKSUMS_SETTING)) {
                    args.put(DISABLE_CHECKSUMS_SETTING, disableChecksums);
                }
                uris = instance.resolve(args, depsInfo, dependencies);
            }
        }
        if (uris == null) {
            return new URI[0];
        } else {
            return uris;
        }
    }

    public static synchronized Map[] listDependencies(ClassLoader cl) {
        Map[] maps = null;
        if (enableGrapes) {
            GrapeEngine instance = getInstance();
            if (instance != null) {
                maps = instance.listDependencies(cl);
            }
        }
        if (maps == null) {
            return new Map[0];
        } else {
            return maps;
        }

    }

    public static synchronized void addResolver(Map<String, Object> args) {
        if (enableGrapes) {
            GrapeEngine instance = getInstance();
            if (instance != null) {
                instance.addResolver(args);
            }
        }
    }
}
