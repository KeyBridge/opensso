/**
 * $Id: ManifestConstants.java,v 1.1 2008-08-08 22:36:23 kevinserwin Exp $
 * Copyright © 2008 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this document.
 * In particular, and without limitation, these intellectual property rights
 * may include one or more of the U.S. patents listed at
 * http://www.sun.com/patents and one or more additional patents or pending
 * patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software.  Government users are subject
 * to the Sun Microsystems, Inc. standard license agreement and applicable
 * provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.Sun,
 * Sun Microsystems and  the Sun logo are trademarks or registered trademarks
 * of Sun Microsystems, Inc. in the U.S. and other countries.  
 *
 * Copyright © 2008 Sun Microsystems, Inc. Tous droits réservés.
 * Sun Microsystems, Inc. détient les droits de propriété intellectuels relatifs
 * à la technologie incorporée dans le produit qui est décrit dans ce document.
 * En particulier, et ce sans limitation, ces droits de propriété
 * intellectuelle peuvent inclure un ou plus des brevets américains listés
 * à l'adresse http://www.sun.com/patents et un ou les brevets supplémentaires
 * ou les applications de brevet en attente aux Etats - Unis et dans les
 * autres pays.
 *
 * L'utilisation est soumise aux termes du contrat de licence.
 *
 * Cette distribution peut comprendre des composants développés par des
 * tierces parties.
 *
 * Sun,  Sun Microsystems et  le logo Sun sont des marques de fabrique ou des
 * marques déposées de Sun Microsystems, Inc. aux Etats-Unis et dans
 * d'autres pays.
 */

package com.sun.identity.tools.manifest;

public interface ManifestConstants {
    
    int BUFFER_SIZE = 8192;
    String SHA1 = "SHA1";
    String DEFAULT_RECURSIVE = "true";
    String DEFAULT_MANIFEST_FILE_NAME = "MANIFEST.MF";
    String EQUAL = "=";
    String FILE_SEPARATOR = "/";
    char DEFAULT_WILD_CARD = '*';
    String PATTERN_SEPARATOR = ",";
    String HEADER_FILE_PATH = "file.header.path";
    String SRC_FILE_PATH = "file.src.path";
    String DEST_FILE_PATH = "file.dest.path";
    String RECURSIVE = "file.recursive";
    String INCLUDE_PATTERN = "file.include";
    String EXCLUDE_PATTERN = "file.exclude";
    String MANIFEST_NAME = "name.manifest";
    String WILDCARD_CHAR = "char.wildcard";
    String DIGEST_ALG = "digest.alg";
    String DIGEST_HANDLEJAR = "digest.handlejar";
    String DEFAULT_DIGEST_HANDLEJAR = "true";
    String DIGEST_HANDLEWAR = "digest.handlewar";
    String DEFAULT_DIGEST_HANDLEWAR = "true";
    String JAR_FILE_EXT = ".jar";
    String WAR_FILE_EXT = ".war";
    String OVERWRITE = "file.overwrite";
    String DEFAULT_OVERWRITE = "true";
    
}
