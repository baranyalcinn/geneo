export const GRAPH_NODE_WIDTH = 200; // Ortalama bir özel düğüm genişliği
export const GRAPH_NODE_HEIGHT = 130; // Ortalama bir özel düğüm yüksekliği

// Dagre tarafından kullanılacak temel ayarlar
export const GRAPH_LAYOUT_RANKDIR = 'TB'; // Yerleşim yönü: TB (Top-to-Bottom), LR (Left-to-Right)
export const GRAPH_LAYOUT_ALIGN = undefined; // Düğüm hizalaması (undefined, UL, UR, DL, DR)
export const GRAPH_LAYOUT_NODESEP = 60;    // Aynı seviyedeki düğümler arası boşluk
export const GRAPH_LAYOUT_RANKSEP = 70;    // Farklı seviyeler (rank) arası boşluk
export const GRAPH_LAYOUT_MARGIN_X = 20;   // Grafik kenar boşluğu X
export const GRAPH_LAYOUT_MARGIN_Y = 20;   // Grafik kenar boşluğu Y

// Kenar tipleri için (opsiyonel, ileride kullanılabilir)
export const EDGE_TYPE_DEFAULT = 'smoothstep';
export const EDGE_TYPE_HIERARCHICAL = 'default'; // Veya dagre'nin desteklediği başka bir tip

// Renkler (Cinsiyet ve ilişki türü için referans olabilir, CustomNode ve Edge stillerinde kullanılacak)
export const GENDER_COLOR_MALE = '#72A0C1'; // Örnek renk
export const GENDER_COLOR_FEMALE = '#F8BBD0'; // Örnek renk
export const GENDER_COLOR_OTHER = '#BDBDBD'; // Örnek renk

export const RELATIONSHIP_COLOR_SPOUSE = '#4CAF50'; // Örnek renk
export const RELATIONSHIP_COLOR_PARENT_CHILD = '#2196F3'; // Örnek renk 