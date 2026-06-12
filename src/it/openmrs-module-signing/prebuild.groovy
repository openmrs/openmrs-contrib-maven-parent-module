/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

// Generate a throwaway GPG key into an isolated keyring before the build signs.
// `gpgHome` is injected via the invoker plugin's scriptVariables and is the same
// directory the forked build sees as GNUPGHOME, so the build signs with this key
// and postbuild.groovy can verify against it. The keyring lives under target/ and
// is recreated each run, so it never touches the developer's real keyring and
// leaves nothing behind. The passphrase matches MAVEN_GPG_PASSPHRASE in
// invoker.properties.
File home = new File(gpgHome)
home.deleteDir()
home.mkdirs()
["chmod", "700", home.absolutePath].execute().waitFor()

File params = new File(home, "key-params")
params.text = '''%echo Generating ephemeral signing key for the signing integration test
Key-Type: RSA
Key-Length: 2048
Subkey-Type: RSA
Subkey-Length: 2048
Name-Real: OpenMRS Signing
Name-Email: signing@openmrs.org
Expire-Date: 0
Passphrase: changeit
%commit
%echo done
'''

Process gen = ["gpg", "--batch", "--pinentry-mode", "loopback", "--homedir", home.absolutePath,
               "--gen-key", params.absolutePath].execute()
StringBuilder err = new StringBuilder()
gen.consumeProcessErrorStream(err)
gen.waitFor()
if (gen.exitValue() != 0) {
	throw new RuntimeException("Failed to generate ephemeral GPG key:\n" + err.toString())
}

println "[signing] generated ephemeral signing key in " + home.absolutePath
return true