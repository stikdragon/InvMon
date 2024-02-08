package uk.co.stikman.invmon.tooling;

import java.io.File;
import java.util.List;

import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

import uk.co.stikman.invmon.inverter.util.InvUtil;

public class Builder {

	public class ProgressThing implements TeaVMProgressListener {

		private TeaVMToolLog	log;
		private long			lastT;
		private int				maxProg;

		public ProgressThing(TeaVMToolLog log) {
			this.log = log;
		}

		@Override
		public TeaVMProgressFeedback progressReached(int arg0) {
			long dt = System.currentTimeMillis() - lastT;
			lastT = System.currentTimeMillis();
			if (dt > 1000)
				System.out.println("  " + (int) (100 * arg0 / maxProg) + " %");
			return TeaVMProgressFeedback.CONTINUE;
		}

		@Override
		public TeaVMProgressFeedback phaseStarted(TeaVMPhase arg0, int arg1) {
			log.info("Starting phase: " + arg0);
			lastT = System.currentTimeMillis();
			maxProg = arg1;
			return TeaVMProgressFeedback.CONTINUE;
		}

	}

	private Args args;

	public static void main(String[] args) throws TeaVMToolException {
		try {
			Builder b = new Builder(new Args(args));
			b.compile();
		} catch (Exception e) {
			System.exit(-1);
		}
	}

	public Builder(Args args) {
		this.args = args;
	}

	public void compile() throws TeaVMToolException {
		long start = System.currentTimeMillis();
		
		String outputdir = "war";
		if (args.exists("output"))
			outputdir = args.getSingle("output");
		
		TeaVMTool tool = new TeaVMTool();
		String obfuscatedJS = args.findSingle("obfuscatedJS");
		tool.setObfuscated(obfuscatedJS != null ? Boolean.parseBoolean(obfuscatedJS) : true);
		TeaVMToolLog log = new ConsoleTeaVMToolLog(true);
		tool.setLog(log);
		tool.setMainClass(args.getSingle("mainclass"));
		tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
		tool.setSourceMapsFileGenerated(Boolean.parseBoolean(args.getSingle("sourcemaps", "false")));
		tool.setTargetDirectory(new File(outputdir));
		tool.getProperties().put("java.util.Locale.available", "en_US, en_GB");
		tool.generate();
		List<Problem> probs = tool.getProblemProvider().getProblems();
		if (!probs.isEmpty()) {
			for (Problem p : probs) {
				DefaultProblemTextConsumer dptc = new DefaultProblemTextConsumer();
				p.render(dptc);
				log.error(dptc.getText());
			}
			throw new TeaVMToolException("Compilation failed");
		}
		log.info(String.format("Complete in %.1fs", (System.currentTimeMillis() - start) / 1000.0f));
		File f = new File(outputdir + "\\classes.js");
		log.info(f.toString() + ": " + InvUtil.formatSize(f.length()));
	}

}