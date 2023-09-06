from flask import Flask, redirect, request
from nbt import nbt
from PIL import Image
from datetime import datetime
import mysql.connector
import os
import json
import requests


center_adjust = [0, 64, 192, 448, 960]
color_dict = [
    (0, 0, 0),
    (127, 178, 56),
    (247, 233, 163),
    (199, 199, 199),
    (255, 0, 0),
    (160, 160, 255),
    (167, 167, 167),
    (0, 124, 0),
    (255, 255, 255),
    (164, 168, 184),
    (151, 109, 77),
    (112, 112, 112),
    (64, 64, 255),
    (143, 119, 72),
    (255, 252, 245),
    (216, 127, 51),
    (178, 76, 216),
    (102, 153, 216),
    (229, 229, 51),
    (127, 204, 25),
    (242, 127, 165),
    (76, 76, 76),
    (153, 153, 153),
    (76, 127, 153),
    (127, 63, 178),
    (51, 76, 178),
    (102, 76, 51),
    (102, 127, 51),
    (153, 51, 51),
    (25, 25, 25),
    (250, 238, 77),
    (92, 219, 213),
    (74, 128, 255),
    (0, 217, 58),
    (129, 86, 49),
    (112, 2, 0),
    (209, 177, 161),
    (159, 82, 36),
    (149, 87, 108),
    (112, 108, 138),
    (186, 133, 36),
    (103, 117, 53),
    (160, 77, 78),
    (57, 41, 35),
    (135, 107, 98),
    (87, 92, 92),
    (122, 73, 88),
    (76, 62, 92),
    (76, 50, 35),
    (76, 82, 42),
    (142, 60, 46),
    (37, 22, 16),
    (189, 48, 49),
    (148, 63, 97),
    (92, 25, 29),
    (22, 126, 134),
    (58, 142, 140),
    (86, 44, 62),
    (20, 180, 133),
    (100, 100, 100),
    (216, 175, 147),
    (127, 167, 150)
]
color_multiplier = [180/255, 220/255, 1, 135/255]


app = Flask(__name__)


def dat_to_info(id):
    source_file = f"C:/Users/ArchWizard7/Desktop/Development/MinecraftServer/[1.20.1]Spigot/MCWebMap/data/map_{id}.dat"

    if not os.path.exists(source_file):
        return

    nbt_file = nbt.NBTFile(source_file, "rb")

    data = nbt_file["data"]
    scale = int(str(data["scale"]))
    x_center = int(str(data["xCenter"])) - center_adjust[scale]
    z_center = int(str(data["zCenter"])) - center_adjust[scale]

    # Leaflet 用に調整
    dimension = str(data["dimension"]).replace("minecraft:", "")
    z = (18 - scale)
    x = (x_center // (128 << scale)) + (131072 >> scale)
    y = (z_center // (128 << scale)) + (131072 >> scale)

    colors = data["colors"]

    print(f"file_name: ./static/tiles/{dimension}_{z}_{x}_{y}.png")
    # print(colors)

    byte_array_to_image(dimension, z, x, y, colors)


def byte_array_to_image(dimension, z, x, y, array):
    image = Image.new("RGB", (128, 128))

    for i in range(128):
        for j in range(128):
            b = array[(i * 128) + j]
            color = color_dict[b // 4]
            new_color = tuple(int(k * color_multiplier[b % 4] // 1) for k in color)
            # print(f"({i}, {j}) = {new_color}, {type(new_color)}")
            image.putpixel((j, i), new_color)

    image.save(f"./static/tiles/{dimension}_{z}_{x}_{y}.png")


def update_chunks_table(id, registered, date):
    cnx = None

    try:
        cnx = mysql.connector.connect(
            user="root",
            password="BTcfrLkK1FFU",
            host="localhost",
            database="mcwebmap"
        )

        cursor = cnx.cursor()
        sql = f"""INSERT INTO chunks (id, registered, date) VALUES ('{id}', '{registered}', '{date}') ON DUPLICATE KEY UPDATE registered = '{registered}', date = '{date}'"""

        print(f"\u001b[31m{sql}\u001b[0m")

        cursor.execute(sql)
        cnx.commit()
        cursor.close()
    except Exception as e:
        print("Error Occurred:", e)
    finally:
        if cnx is not None and cnx.is_connected():
            cnx.close()


def get_waypoints_from_mysql(environment):
    cnx = None

    try:
        cnx = mysql.connector.connect(
            user="root",
            password="BTcfrLkK1FFU",
            host="localhost",
            database="mcwebmap"
        )

        cursor = cnx.cursor()
        sql = f"""SELECT * FROM waypoints WHERE environment = '{environment}'"""

        print(f"\u001b[31m{sql}\u001b[0m")

        cursor.execute(sql)
        result = cursor.fetchall()

        data_list = []

        for row in result:
            data_dict = {
                "id": row[0],
                "name": row[1],
                "registered": row[2],
                "date": row[3],
                "environment": row[4],
                "x": row[5],
                "y": row[6],
                "z": row[7],
                "icon": row[8]
            }
            data_list.append(data_dict)

        print(f"\u001b[32m{data_list}\u001b[0m")

        json_data = json.dumps(data_list, indent=4, default=str)

        cursor.close()

        return str(json_data)
    except Exception as e:
        print("Error Occurred:", e)
    finally:
        if cnx is not None and cnx.is_connected():
            cnx.close()


def get_username(uuid):
    url = f"https://sessionserver.mojang.com/session/minecraft/profile/{uuid}"
    response = requests.get(url)

    if response.status_code == 200:
        json_data = json.loads(response.text)
        return json_data["name"]
    else:
        return "An error has occurred while get_username"


@app.route("/")
def root():
    return redirect("/static/index.html")


@app.route("/register-tile", methods=["GET"])
def register_map():
    args = request.args
    now = datetime.now()

    id = args.get("id")
    registered = args.get("registered")
    date = str(now.strftime("%Y/%m/%d %H:%M:%S"))

    # tile を png に保存
    dat_to_info(id)

    # 空データであるかチェック
    if id is not None and registered is not None:
        # MySQL の chunks テーブルに追加/更新
        update_chunks_table(id, registered, date)

        return "Data Inserted!", 200
    else:
        return "Invalid Query", 400


@app.route("/get-waypoints", methods=["GET"])
def get_waypoints():
    args = request.args

    environment = args.get("environment")

    if environment is None:
        return "Bad Request", 400
    else:
        return get_waypoints_from_mysql(environment), 200, {
            "Content-Type": "application/json"
        }


@app.route("/get-username-from-uuid")
def get_username_from_uuid():
    args = request.args

    uuid = args.get("uuid")

    if uuid is None:
        return "Bad Request", 400
    else:
        return get_username(uuid), 200, {
            "Content-Type": "text/plain"
        }


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)

