// Re-grab the metadata for available quarks
Quarks2.refresh

// This is for dev purposes: load the quark info from disk into
a = Quarks2.getQuarksInfo
a.keys
a["cruciallib"].keysValuesDo{|k,v| "ENTRY ".post; k.postln; v.postln;}
a["cruciallib"]["version"]

// Not implemented yet - this downloads the requested version
Quarks2.fetchQuark("cruciallib", "3.4", "0.2")
// Not implemented yet
Quarks2.installQuark("cruciallib", "3.4", "0.2")


//this should work:

//hosted at https://github.com/miguel-negrao/Quarks2Test
Quarks2.fetchQuark("Quarks2Test", "3.5", "0.1")

Quarks2.install("Quarks2Test","3.5", "0.1")
//will use 0.1
Quarks2.install("Quarks2Test","3.5")

//will fail
Quarks2.install("Quarks2Test")

//this is part of the Quarks2test.
Test123.test
