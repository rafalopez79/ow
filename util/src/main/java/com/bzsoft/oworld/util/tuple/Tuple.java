package com.bzsoft.oworld.util.tuple;

public final class Tuple {

	private Tuple() {
		// empty
	}

	public static final <A, B> Tuple2<A, B> of(A a, B b) {
		return new Tuple2<>(a, b);
	}

	public static final <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
		return new Tuple3<>(a, b, c);
	}

	public static class Tuple2<A, B> {

		protected final A a;
		protected final B b;

		protected Tuple2(final A a, B b) {
			this.a = a;
			this.b = b;
		}

		public A get1() {
			return a;
		}

		public B get2() {
			return b;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof Tuple2) {
				final Tuple2<?, ?> p = (Tuple2<?, ?>) obj;
				return (p.a == null && a == null || p.a != null && a != null && p.a.equals(a))
						&& (p.b == null && b == null || p.b != null && b != null && p.b.equals(b));
			}
			return false;
		}

		@Override
		public int hashCode() {
			int hashcode = 0;
			if (a != null) {
				hashcode += a.hashCode();
			}
			if (b != null) {
				hashcode += b.hashCode();
			}
			return hashcode;
		}
	}

	public static class Tuple3<A, B, C> extends Tuple2<A, B> {

		protected final C c;

		protected Tuple3(final A a, B b, C c) {
			super(a, b);
			this.c = c;
		}

		public C get3() {
			return c;
		}

		@Override
		public boolean equals(Object obj) {
			boolean eq = super.equals(obj);
			if (eq && obj instanceof Tuple3) {
				final Tuple3<?, ?, ?> p = (Tuple3<?, ?, ?>) obj;
				eq = p.c == null && c == null || p.c != null && c != null && p.c.equals(c);
			}
			return eq;
		}

		@Override
		public int hashCode() {
			int hashcode = super.hashCode();
			if (c != null) {
				hashcode += c.hashCode();
			}
			return hashcode;
		}
	}

	public static class Tuple4<A, B, C, D> extends Tuple3<A, B, C> {

		protected final D d;

		protected Tuple4(final A a, B b, C c, D d) {
			super(a, b, c);
			this.d = d;
		}

		public D get4() {
			return d;
		}

		@Override
		public boolean equals(Object obj) {
			boolean eq = super.equals(obj);
			if (eq && obj instanceof Tuple4) {
				final Tuple4<?, ?, ?, ?> p = (Tuple4<?, ?, ?, ?>) obj;
				eq = p.d == null && d == null || p.d != null && d != null && p.d.equals(d);
			}
			return eq;
		}

		@Override
		public int hashCode() {
			int hashcode = super.hashCode();
			if (d != null) {
				hashcode += d.hashCode();
			}
			return hashcode;
		}
	}
}
