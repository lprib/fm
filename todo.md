# TODO

## Synth engine
- [ ] Investigate frequency modulation increasing in effect over time
- [ ] Topological sort
- [ ] MIDI input
- [ ] Unison span input and unison support
- [ ] MIDI mod wheel input?
- [ ] Detect long runs of DspNode that do not depend on anything else and run in parallel
- [ ] Support for attenuation on port inputs
- [ ] Lower level compilation to remove all the indirection of port.read(state). Ie. reduce DspNodes to single functions which can be chained?

## Node editor
- [x] allow metadata tags for nodes (color, custom name, etc.)
- [ ] Generic node metadata, rather than hardcoded types
- [ ] crashed on file not found
- [ ] set working directory, patch browser?
- [x] default value for node ports on initialization of node
- [ ] separate compilation into transformation from (Node, Link) to (Node) and JSON serializing (2 steps)
