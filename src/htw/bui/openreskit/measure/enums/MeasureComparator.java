package htw.bui.openreskit.measure.enums;

import htw.bui.openreskit.domain.measure.Measure;

import java.util.Comparator;

public enum MeasureComparator implements Comparator<Measure> {
	ID {
		public int compare(Measure o1, Measure o2) {
			return Integer.valueOf(o1.getId()).compareTo(o2.getId());
		}},
		NAME {
			public int compare(Measure o1, Measure o2) {
				return o1.getName().compareTo(o2.getName());
			}},
			CREATION_DATE{
				public int compare(Measure o1, Measure o2) {
					return o1.getCreationDate().compareTo(o2.getCreationDate());
				}},
				DUE_DATE{
					public int compare(Measure o1, Measure o2) {
						return o1.getDueDate().compareTo(o2.getDueDate());
					}},
					PRIORITY{
						public int compare(Measure o1, Measure o2) {
							return Integer.valueOf(o1.getPriority()).compareTo(o2.getPriority());
						}},
						STATUS{
							public int compare(Measure o1, Measure o2) {
								return Integer.valueOf(o1.getStatus()).compareTo(o2.getPriority());
							}};            

							public static Comparator<Measure> decending(final Comparator<Measure> other) {
								return new Comparator<Measure>() {
									public int compare(Measure o1, Measure o2) {
										return -1 * other.compare(o1, o2);
									}
								};
							}

							public static Comparator<Measure> getComparator(final MeasureComparator... multipleOptions) {
								return new Comparator<Measure>() {
									public int compare(Measure o1, Measure o2) {
										for (MeasureComparator option : multipleOptions) {
											int result = option.compare(o1, o2);
											if (result != 0) {
												return result;
											}
										}
										return 0;
									}
								};
							}
}
