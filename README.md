# mybitmap
改造roaringbitmap，适应imei存取。主要对Container进行改造，使原来16bit支持32bit，因此，总共可以支持48bit长度的无符号整数，可以满足IMEI（除去前面固定的86）
