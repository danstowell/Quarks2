Quarks2
=======

sketch of new quarks implementation for supercollider.

Install this folder into your SC Extensions.

See example_quarks2.scd for test code.



Notes re YAML data representation:
---------------------------------

For a quark, the "version" entry should be a sub-dictionary with fixed version numbers as keys. On the other hand, to provide info about your current latest version (the "HEAD" or development version, perhaps), you do not put this inside "version" but in the root. For example:

{
crazylib: {
        version: {
                0.2: {
                        compat: [3.6],
                        fetchInfo: {tag: "V0.2final"}
                },
	fetchInfo: {tag: "latest"}       # usually don't need special fetchInfo for the default bleeding-edge checkout, but you'd put it here if you need it
}

In many cases you do NOT need to add fetchInfo at the root - for example, by default the \git method simply fetches the "master" branch, which in most cases is fine.

