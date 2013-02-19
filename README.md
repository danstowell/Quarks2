Quarks2
=======

sketch of new quarks implementation for supercollider.

Install this folder into your SC Extensions.

See `example_quarks2.scd` for test code.

Notes on quark versioning
-------------------------

A user can ask to install/fetch either:

* a specific numbered version of a quark - that version is installed. In future it may be updated, but if a new version becomes available it won't be upgraded.
* no specific version - the latest suitable version is installed, and in future it can upgrade to newer versions.

On disk, the "cupboard" folder contains folders with quarkversion number at the end, or "-latest" for the "upgradeable" version. Once the YAML metadata is loaded into the language, this is translated into either a floating-point number or nil (to refer to the default upgradeable version).


QUARK AUTHORS do not need to provide "versions" info - by default, just one single default version is used. The most important time you need multiple versions is when there are different compatibility criteria.

For a quark, the "versions" entry should be a sub-dictionary with fixed version numbers as keys. On the other hand, to provide info about your current latest version (the "HEAD" or development version, perhaps), you do not put this inside "versions" but in the root. For example:

	{
	crazylib: {
	        versions: {
	                0.2: {
	                        compat: [3.6],
	                        fetchInfo: {tag: "V0.2final"}
	                },
		fetchInfo: {tag: "latest"}       # usually don't need special fetchInfo for the default bleeding-edge checkout, but you'd put it here if you need it
	}

In many cases you do NOT need to add fetchInfo at the root - for example, by default the \git method simply fetches the "master" branch, which in most cases is fine. "compat" is a list of SC "major.minor" versions that this quark version is compatible with (if missing, compatible with all). Similarly, the key "platform" can be a list of specific platform compatibilities (eg ["mac", "windows", "linux"]).


Dependencies
------------

If a quark depends on other quarks, here's how to specify it:

	{
	crazylib: {
		dependencies: [
			{ name: "bonkerslib" },
			{ name: "yabalab", version: 0.4 }
			]
		}
	}


