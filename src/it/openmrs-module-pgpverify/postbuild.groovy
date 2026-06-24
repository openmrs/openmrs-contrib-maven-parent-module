/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

// Prove the parent actually wires the pgpverify plugin into a module build. Two
// independent signals confirm the verify goal ran: Maven prints a goal-execution
// banner ("--- openmrs-pgpverify:<ver>:verify (...) @ <module> ---", using the
// plugin's goal prefix rather than its full artifactId) immediately before invoking
// the mojo, and the mojo itself logs a "PGP verification checked N ..." summary once
// its logic completes. Either line is sufficient evidence; both are matched loosely
// so this keeps working across plugin upgrades. Whether the goal passes or fails
// verification is out of scope -- that is covered by the plugin's own integration
// tests.
File log = new File(basedir, "build.log")
if (!log.exists()) {
	throw new RuntimeException("Forked build log is missing: " + log.absolutePath)
}

def banner = ~/openmrs-pgpverify(?:-maven-plugin)?:[^:\s]+:verify/
def summary = ~/PGP verification checked \d+/
boolean ran = log.readLines().any { banner.matcher(it).find() || summary.matcher(it).find() }
if (!ran) {
	throw new RuntimeException("pgpverify:verify did not run during the module build; "
			+ "no plugin execution banner or summary found in " + log.absolutePath)
}

println "[pgpverify] confirmed the pgpverify:verify goal executed during the module build"
return true
