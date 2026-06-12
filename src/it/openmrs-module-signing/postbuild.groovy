/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

// Prove the GPG signing actually worked: every signature produced by the build
// must (a) have its signed artifact present and (b) pass `gpg --verify` against
// the ephemeral public key prebuild.groovy generated. A good signature exits 0
// even though the key is untrusted, so verification passing here means the bytes
// were genuinely signed by that key. `gpgHome` is the same keyring the build
// signed with, injected via the invoker plugin's scriptVariables.
File home = new File(gpgHome)

List<File> signatures = []
["api/target", "omod/target"].each { rel ->
	File dir = new File(basedir, rel)
	if (dir.isDirectory()) {
		dir.listFiles().each { File f ->
			if (f.isFile() && f.name.endsWith(".asc")) {
				signatures << f
			}
		}
	}
}

if (signatures.isEmpty()) {
	throw new RuntimeException("No .asc signatures were produced; GPG signing did not run")
}

signatures.each { File asc ->
	File signed = new File(asc.parentFile, asc.name.substring(0, asc.name.length() - ".asc".length()))
	if (!signed.exists()) {
		throw new RuntimeException("Signed artifact is missing for signature " + asc.name)
	}
	Process proc = ["gpg", "--homedir", home.absolutePath, "--batch", "--verify",
	                asc.absolutePath, signed.absolutePath].execute()
	StringBuilder err = new StringBuilder()
	proc.consumeProcessErrorStream(err)
	proc.waitFor()
	if (proc.exitValue() != 0) {
		throw new RuntimeException("gpg --verify failed for " + asc.name + ":\n" + err.toString())
	}
	println "[signing] verified signature: " + asc.name
}

// Guard against a build that only signs incidental artifacts: the .omod (the
// module's primary artifact, whose file the openmrs plugin swaps in for the jar)
// and the POM must both be among the signed files.
List<String> names = signatures.collect { it.name }
if (!names.any { it.endsWith(".omod.asc") }) {
	throw new RuntimeException("The .omod artifact was not signed: " + names)
}
if (!names.any { it.endsWith(".pom.asc") }) {
	throw new RuntimeException("No POM signature was produced: " + names)
}

println "[signing] verified " + signatures.size() + " GPG signatures"
return true