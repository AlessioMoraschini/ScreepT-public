package om;

public enum Difficulty {
	SUPER_EASY(1, 0.6d),
	EASY(1, 0.8d),
	MIDDLE(1, 1.0d),
	HARD(2, 1.1d),
	VERY_HARD(2, 1.2d),
	SUPERHERO(3, 1.4d);
	
	public int multiplierI;
	public double multiplierD;
	
	private Difficulty(int multiplierI, double multiplierD) {
		this.multiplierI = multiplierI;
		this.multiplierD = multiplierD;
	}
}
