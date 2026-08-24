{
	"format_version": "1.12.0",
	"minecraft:geometry": [
		{
			"description": {
				"identifier": "geometry.guide_core",
				"texture_width": 64,
				"texture_height": 64,
				"visible_bounds_width": 2,
				"visible_bounds_height": 2,
				"visible_bounds_offset": [0, 0, 0]
			},
			"bones": [
				{
					"name": "corpus",
					"pivot": [0, 0, 0],
					"cubes": [
						{"origin": [-3, -5, -3], "size": [6, 10, 6], "uv": [0, 0]}
					]
				},
				{
					"name": "core",
					"pivot": [0, 0, 0],
					"cubes": [
						{"origin": [-2, -2, -2], "size": [4, 4, 4], "uv": [48, 0]},
						{"origin": [-2, -2, -2], "size": [4, 4, 4], "pivot": [0, 0, 0], "rotation": [0, -45, 0], "uv": [48, 0]}
					]
				}
			]
		}
	]
}