/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

// Signing requires a working `gpg` executable to generate the ephemeral key and
// to sign artifacts. On a dev machine without gpg we skip the test rather than
// fail the build. In CI we must not skip silently: this test exists to guarantee
// signing works, so a missing gpg there is a hard error, not a quiet pass. CI
// providers (including GitHub Actions) set CI=true, so we fail loudly when gpg is
// absent under CI and skip otherwise.
boolean ci = "true".equalsIgnoreCase(System.getenv("CI"))

boolean gpgAvailable
try {
	Process p = ["gpg", "--version"].execute()
	p.waitFor()
	gpgAvailable = p.exitValue() == 0
} catch (IOException e) {
	gpgAvailable = false
}

if (gpgAvailable) {
	return true
}

if (ci) {
	throw new IllegalStateException("gpg is required to run the signing IT in CI but was not found on PATH")
}

println "[signing] gpg not available; skipping signing IT (set CI=true to make this a hard failure)"
return false