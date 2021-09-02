# TODO

## Synth engine
- [ ] Polyphony
	- Threading per voice
	- Voices need to know when they start and stop producing sounds (ie. a
	  'master' ADSR that tracks gate and release). Should be configurable per
	  unison program, so that when the last unison finishes, the thread sleeps.
	- When a voice is not active, the thread sleeps
- [ ] Unison span input and unison support
	- Add a pan left/right node to easily spread unison voices around the stereo field
- [ ] State based ADSR, ie. state = {Attacking, decaying, sustaining,
  releasing, off}. Avoids discontinuities, allows other lerping behaviour (quadratic?)
- [ ] Hotloading of programs... Watch for file change? or run http server to upload JSON?
- [ ] Investigate frequency modulation increasing in effect over time
- [ ] Topological sort
- [x] MIDI input
- [ ] MIDI mod wheel input?
- [x] Detect long runs of DspNode that do not depend on anything else and run
  in parallel (WONT DO - run voicec in parallel instaed)
- [ ] Support for attenuation on port inputs
- [ ] Lower level compilation to remove all the indirection of
  port.read(state). Ie. reduce DspNodes to single functions which can be
  chained?
- [ ] Write node editor also in rust
	- Real-time wave visualizations:
		- For osc, iterate t = 0 to 2pi. Find all parents in the dependency
		  chain, and run them (modulators)
		- Alternatively, bypass every ADSR (always output 1), and run each
		  oscillator in dependency order from 0 to 2pi. Use the cached output
		  of previous nodes to form input modulation for the current node.
		- This requires separating out `t` from DspNodes, so they can be
		  executed in a 'mock' context by the wave visualizer.
- [ ] If running synth as a server, it can return waveform objects back to the
  client (processing) to render per-oscillator waveforms.

## Node editor
- [x] allow metadata tags for nodes (color, custom name, etc.)
- [ ] Generic node metadata, rather than hardcoded types
- [ ] crashed on file not found
- [ ] set working directory, patch browser?
- [x] default value for node ports on initialization of node
- [ ] separate compilation into transformation from (Node, Link) to (Node) and JSON serializing (2 steps)
