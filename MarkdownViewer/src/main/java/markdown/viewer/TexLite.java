/*
 * Copyright (c) AM-Design-Development. All rights reserved.
 */
package markdown.viewer;

import java.util.HashMap;
import java.util.Map;

/**
 * Tiny LaTeX math to html 3.2 converter for the Swing preview (no dependencies): greek letters and the common symbols
 * become unicode, ^ and _ become sup / sub, \frac, \sqrt, \text, \mathbf, \left / \right and environments are
 * simplified. It is a readable approximation: the exported html page renders the formulas with KaTeX.
 */
public final class TexLite {

	private static final Map<String, String> SYMBOLS = new HashMap<String, String>();

	static {
		String[][] table = {
			{"alpha", "α"}, {"beta", "β"}, {"gamma", "γ"}, {"delta", "δ"}, {"epsilon", "ϵ"}, {"varepsilon", "ε"}, {"zeta", "ζ"},
			{"eta", "η"}, {"theta", "θ"}, {"vartheta", "ϑ"}, {"iota", "ι"}, {"kappa", "κ"}, {"lambda", "λ"}, {"mu", "μ"},
			{"nu", "ν"}, {"xi", "ξ"}, {"pi", "π"}, {"varpi", "ϖ"}, {"rho", "ρ"}, {"varrho", "ϱ"}, {"sigma", "σ"},
			{"varsigma", "ς"}, {"tau", "τ"}, {"upsilon", "υ"}, {"phi", "ϕ"}, {"varphi", "φ"}, {"chi", "χ"}, {"psi", "ψ"},
			{"omega", "ω"}, {"Gamma", "Γ"}, {"Delta", "Δ"}, {"Theta", "Θ"}, {"Lambda", "Λ"}, {"Xi", "Ξ"}, {"Pi", "Π"},
			{"Sigma", "Σ"}, {"Upsilon", "Υ"}, {"Phi", "Φ"}, {"Psi", "Ψ"}, {"Omega", "Ω"},
			{"sum", "∑"}, {"prod", "∏"}, {"coprod", "∐"}, {"int", "∫"}, {"iint", "∬"}, {"iiint", "∭"}, {"oint", "∮"},
			{"infty", "∞"}, {"partial", "∂"}, {"nabla", "∇"}, {"forall", "∀"}, {"exists", "∃"}, {"nexists", "∄"},
			{"emptyset", "∅"}, {"varnothing", "∅"}, {"in", "∈"}, {"notin", "∉"}, {"ni", "∋"}, {"subset", "⊂"},
			{"supset", "⊃"}, {"subseteq", "⊆"}, {"supseteq", "⊇"}, {"cup", "∪"}, {"cap", "∩"}, {"bigcup", "⋃"},
			{"bigcap", "⋂"}, {"setminus", "∖"}, {"pm", "±"}, {"mp", "∓"}, {"times", "×"}, {"div", "÷"}, {"cdot", "·"},
			{"ast", "∗"}, {"star", "⋆"}, {"circ", "∘"}, {"bullet", "•"}, {"oplus", "⊕"}, {"otimes", "⊗"},
			{"leq", "≤"}, {"le", "≤"}, {"geq", "≥"}, {"ge", "≥"}, {"neq", "≠"}, {"ne", "≠"}, {"approx", "≈"},
			{"equiv", "≡"}, {"sim", "∼"}, {"simeq", "≃"}, {"cong", "≅"}, {"propto", "∝"}, {"ll", "≪"}, {"gg", "≫"},
			{"to", "→"}, {"rightarrow", "→"}, {"leftarrow", "←"}, {"leftrightarrow", "↔"}, {"Rightarrow", "⇒"},
			{"Leftarrow", "⇐"}, {"Leftrightarrow", "⇔"}, {"implies", "⟹"}, {"iff", "⟺"}, {"mapsto", "↦"},
			{"uparrow", "↑"}, {"downarrow", "↓"}, {"longrightarrow", "⟶"}, {"land", "∧"}, {"wedge", "∧"}, {"lor", "∨"},
			{"vee", "∨"}, {"neg", "¬"}, {"lnot", "¬"}, {"top", "⊤"}, {"bot", "⊥"}, {"perp", "⊥"}, {"parallel", "∥"},
			{"angle", "∠"}, {"triangle", "△"}, {"degree", "°"}, {"prime", "′"}, {"hbar", "ℏ"}, {"ell", "ℓ"},
			{"Re", "ℜ"}, {"Im", "ℑ"}, {"aleph", "ℵ"}, {"ldots", "…"}, {"dots", "…"}, {"cdots", "⋯"}, {"vdots", "⋮"},
			{"ddots", "⋱"}, {"langle", "⟨"}, {"rangle", "⟩"}, {"lceil", "⌈"}, {"rceil", "⌉"}, {"lfloor", "⌊"},
			{"rfloor", "⌋"}, {"vert", "|"}, {"mid", "∣"}, {"|", "‖"}, {"Vert", "‖"}, {"{", "{"}, {"}", "}"}, {"%", "%"},
			{"$", "$"}, {"&", "&amp;"}, {"#", "#"}, {"_", "_"}, {",", " "}, {";", " "}, {":", " "},
			{"!", ""}, {" ", " "}, {"quad", " "}, {"qquad", "  "}, {"\\", "<br>"},
			{"sin", "sin"}, {"cos", "cos"}, {"tan", "tan"}, {"cot", "cot"}, {"sec", "sec"}, {"csc", "csc"},
			{"arcsin", "arcsin"}, {"arccos", "arccos"}, {"arctan", "arctan"}, {"sinh", "sinh"}, {"cosh", "cosh"},
			{"tanh", "tanh"}, {"log", "log"}, {"ln", "ln"}, {"lg", "lg"}, {"exp", "exp"}, {"lim", "lim"}, {"max", "max"},
			{"min", "min"}, {"sup", "sup"}, {"inf", "inf"}, {"det", "det"}, {"gcd", "gcd"}, {"deg", "deg"}, {"dim", "dim"},
			{"mod", "mod"}, {"bmod", "mod"}, {"arg", "arg"}, {"ker", "ker"},
		};
		for(String[] s : table)
			SYMBOLS.put(s[0], s[1]);
	}

	private final String tex;
	private int pos;

	private TexLite(String tex) {
		this.tex = tex;
	}

	/** Html (sup / sub / i / b and unicode) for the formula. */
	public static String toHtml(String tex) {
		try {
			return new TexLite(tex == null ? "" : tex.trim()).parseUntil('\0');
		} catch (RuntimeException e) {
			return MarkdownText.htmlEscape(tex);
		}
	}

	private String parseUntil(char end) {
		StringBuilder sb = new StringBuilder();
		while(pos < tex.length()) {
			char c = tex.charAt(pos);
			if(c == end) {
				pos++;
				return sb.toString();
			}
			sb.append(atom());
			if(pos < tex.length() && (tex.charAt(pos) == '^' || tex.charAt(pos) == '_'))
				sb.append(scripts());
		}
		return sb.toString();
	}

	private String scripts() {
		StringBuilder sb = new StringBuilder();
		while(pos < tex.length() && (tex.charAt(pos) == '^' || tex.charAt(pos) == '_')) {
			char kind = tex.charAt(pos++);
			skipSpaces();
			String arg = argument();
			sb.append(kind == '^' ? "<sup>" + arg + "</sup>" : "<sub>" + arg + "</sub>");
		}
		return sb.toString();
	}

	/** One unit: a {group}, a \command (with its arguments) or a character. */
	private String atom() {
		char c = tex.charAt(pos);
		if(c == '{') {
			pos++;
			return parseUntil('}');
		}
		if(c == '\\')
			return command();
		pos++;
		switch(c) {
			case '<': return "&lt;";
			case '>': return "&gt;";
			case '&': return " ";
			case '~': return " ";
			case '\'': return "′";
			case '-': return "−";
			case '*': return "∗";
			case '\n': case '\r': case '\t': case ' ': return "";
			case '+': case '=': return " " + c + " ";
			default:
				return Character.isLetter(c) ? "<i>" + c + "</i>" : String.valueOf(c);
		}
	}

	private String argument() {
		skipSpaces();
		if(pos >= tex.length())
			return "";
		return atom();
	}

	private String rawArgument() {
		skipSpaces();
		if(pos < tex.length() && tex.charAt(pos) == '{') {
			int depth = 0;
			int start = pos + 1;
			for(; pos < tex.length(); pos++) {
				char c = tex.charAt(pos);
				if(c == '{')
					depth++;
				else if(c == '}' && --depth == 0) {
					pos++;
					return tex.substring(start, pos - 1);
				}
			}
			return tex.substring(start);
		}
		return pos < tex.length() ? String.valueOf(tex.charAt(pos++)) : "";
	}

	private String command() {
		pos++; // backslash
		if(pos >= tex.length())
			return "\\";
		int start = pos;
		if(Character.isLetter(tex.charAt(pos))) {
			while(pos < tex.length() && Character.isLetter(tex.charAt(pos)))
				pos++;
		} else {
			pos++;
		}
		String name = tex.substring(start, pos);
		switch(name) {
			case "frac": case "dfrac": case "tfrac": {
				String num = argument(), den = argument();
				return "(" + num + ")/(" + den + ")";
			}
			case "sqrt": {
				skipSpaces();
				String index = "";
				if(pos < tex.length() && tex.charAt(pos) == '[') {
					int close = tex.indexOf(']', pos);
					index = "<sup>" + MarkdownText.htmlEscape(tex.substring(pos + 1, close)) + "</sup>";
					pos = close + 1;
				}
				return index + "√(" + argument() + ")";
			}
			case "text": case "textrm": case "mathrm": case "operatorname": case "mbox":
				return MarkdownText.htmlEscape(rawArgument());
			case "mathbf": case "textbf": case "boldsymbol": case "bm":
				return "<b>" + argument() + "</b>";
			case "mathit": case "textit":
				return "<i>" + argument() + "</i>";
			case "mathbb":
				return doubleStruck(rawArgument());
			case "mathcal": case "mathscr": case "mathfrak": case "mathsf": case "mathtt":
				return argument();
			case "overline": case "bar":
				return argument() + "̅";
			case "hat": case "widehat":
				return argument() + "̂";
			case "vec":
				return argument() + "⃗";
			case "dot":
				return argument() + "̇";
			case "tilde": case "widetilde":
				return argument() + "̃";
			case "left": case "right": case "big": case "Big": case "bigg": case "Bigg": case "bigl": case "bigr": case "Bigl": case "Bigr":
				skipSpaces();
				if(pos < tex.length() && tex.charAt(pos) == '.') {
					pos++;
					return "";
				}
				return "";
			case "begin": case "end":
				rawArgument();
				return name.equals("begin") ? "" : "";
			case "displaystyle": case "textstyle": case "limits": case "nolimits":
				return "";
			default:
				String symbol = SYMBOLS.get(name);
				if(symbol != null)
					return symbol;
				return MarkdownText.htmlEscape("\\" + name);
		}
	}

	private static String doubleStruck(String s) {
		StringBuilder sb = new StringBuilder();
		for(char c : s.toCharArray()) {
			switch(c) {
				case 'N': sb.append('ℕ'); break;
				case 'Z': sb.append('ℤ'); break;
				case 'Q': sb.append('ℚ'); break;
				case 'R': sb.append('ℝ'); break;
				case 'C': sb.append('ℂ'); break;
				case 'P': sb.append('ℙ'); break;
				case 'H': sb.append('ℍ'); break;
				default: sb.append(c);
			}
		}
		return sb.toString();
	}

	private void skipSpaces() {
		while(pos < tex.length() && Character.isWhitespace(tex.charAt(pos)))
			pos++;
	}
}
